package com.github.skopylov58.retry;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Interface to perform asynchronous retry operations on supplier
 * or runnable that may throw an exception.
 * <p>
 * Retry.of(...) factory methods create {@link Worker} from
 * {@link CheckedSupplier} or {@link CheckedRunnable}
 * <p>
 * Sample usage
 * 
 * <pre>
 *      CompletableFuture&lt;Connection&gt; futureConnection = 
 *      Retry.of(() -&gt; DriverManager.getConnection("jdbc:mysql:a:b"))
 *      .withHandler(...)
 *      .withExecutor(...)
 *      .retry();
 * </pre>
 * 
 * @author Sergey Kopylov
 * @author skopylov@gmail.com
 *
 */
public interface Retry {
    
    /**
     * Handles errors and manages retry process flow.
     */
    @FunctionalInterface
    /** Retry handler. */
    interface Handler {
        /**
         * Handles retries during retry process.
         * 
         * @param currentTry current try starting with 0
         * @param exception  exception occurred during this current try
         * @return optional duration to wait until next retry, optional empty signals to stop retrying.
         */
        Optional<Duration> handle(long currentTry, Throwable exception);
        
        /**
         * Simple retry handler which retries fixed number of times with fixed time interval.
         * @param maxTries maximum number of tries
         * @param tryInterval interval in between tries.
         * @return simple handler
         */
        public static Handler simple(final long maxTries, final Duration tryInterval) {
            return (i, th) -> {
                return (i < maxTries) ? Optional.of(tryInterval) : Optional.empty();
            };
        }

        /**
         * Retry handler which will retry forever with fixed time interval.
         * @param tryInterval interval between tries
         * @return handler
         */
        public static Handler forever(final Duration tryInterval) {
            return withFixedInterval(tryInterval);
        }

        public static Handler withInterval(Function<Long, Duration> func) {
            return (i, th) ->  Optional.ofNullable(func.apply(i));
        }

        public static Handler withFixedInterval(Duration dur) {
            return (i, th) ->  Optional.of(dur);
        }
        
        /**
         * Combinator, adds bi-consumer for the handler. May be useful for separating
         * error logging functionality. 
         * @param bi bi-consumer
         * @return
         */
        default Handler also(BiConsumer<Long, Throwable> bi) {
            return (i, th) -> {
                bi.accept(i, th);
                return handle(i, th);
            };
        }

        default Handler withCounter(Predicate<Long> p) {
            return (i, th) -> {
                if (p.test(i)) {
                    return handle(i, th);
                } else {
                    return Optional.empty();
                }
            };
        }

        default Handler withException(Predicate<Throwable> p) {
            return (i, th) -> {
                if (p.test(th)) {
                    return handle(i, th);
                } else {
                    return Optional.empty();
                }
            };
        }
    }
    
    /**
     * Exponential backoff function. 
     * @param i try number starting with 0
     * @param initial initial retry interval
     * @param max maximum retry interval
     * @param multFactor the multiplicative factor or "base", 2 for binary exponential backoff
     * @return retry interval
     */
    static Duration exponentialBackoff(long i, Duration initial, Duration max, int multFactor) {
        double factor = Math.pow(multFactor, i);
        Duration multipliedBy = initial.multipliedBy((long)factor);
        if (multipliedBy.compareTo(max) > 0) {
            return max;
        } else {
            return multipliedBy;
        }
    }

    /** Supplier that may throw an exception. */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /** Supplier that may throw an exception. */
    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * Factory method to creates {@link Worker} for the exceptional supplier.
     * 
     * @param <T>      supplier's result type
     * @param supplier exceptional supplier
     * @return {@link Worker}
     */
    static <T> Worker<T> of(CheckedSupplier<T> supplier) {
        return new Worker<>(() -> {
            try {
                return supplier.get();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Factory method to creates {@link Worker} for exceptional runnable.
     * 
     * @param runnable exceptional runnable
     * @return {@link Worker}
     */
    static Worker<Void> of(CheckedRunnable runnable) {
        return new Worker<>(() -> {
            try{
                runnable.run();
                return null;
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Retry worker, class that actually performs asynchronous retries.
     *
     * @param <T> result type
     */
    class Worker<T> {
        private long currentTry;
        private final Supplier<T> supplier;
        private Handler errorHandler = Handler.simple(10, Duration.ofSeconds(1));
        private Executor executor = ForkJoinPool.commonPool();
        private final CompletableFuture<T> result = new CompletableFuture<>();

        /**
         * Constructor.
         * 
         * @param supplier
         */
        private Worker(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public Worker<T> withHandler(Handler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Worker<T> withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }
        
        public CompletableFuture<T> retry() {
            tryOnce();
            return result;
        }

        private void tryOnce() {
            CompletableFuture.supplyAsync(supplier, executor).whenComplete(this::whenComplete);
        }

        private void whenComplete(T res, Throwable t) {
            if (result.isCancelled()) {
                return;
            }
            if (t != null) {
                handleError(t);
            } else {
                result.complete(res);
            }
        }

        private void handleError(Throwable t) {
            try {
                Optional<Duration> waitDur = errorHandler.handle(currentTry++, t);
                if (waitDur.isEmpty()) {
                    result.completeExceptionally(t);
                } else {
                    CompletableFuture
                    .delayedExecutor(waitDur.get().toMillis(), TimeUnit.MILLISECONDS)
                    .execute(this::tryOnce);
                }
            } catch (Throwable th) {
                result.completeExceptionally(th);
            }
        }
    }
    
}
