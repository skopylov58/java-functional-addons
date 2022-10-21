package com.github.skopylov58.retry;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
 *      .withBackoff(...)
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
     * Exponential backoff function. 
     * <p>
     * delay<sub>i</sub> = initial * multFactor<sup>i</sup>
     * 
     * 
     * @param i try number starting with 0
     * @param initial initial retry interval
     * @param max maximum retry interval
     * @param multFactor the multiplicative factor or "base", 2 for binary exponential backoff
     * @return retry interval = initial * multFactor<sup>i</sup>
     */
    static Duration exponentialBackoff(long i, Duration initial, Duration max, double multFactor) {
        if (multFactor <= 1d) {
            throw new IllegalArgumentException("Expecting multFactor greater 1");
        }
        double factor = Math.pow(multFactor, i);
        if (Double.isNaN(factor) || Double.isInfinite(factor)) {
            return max;
        }
        double resDouble = factor * initial.toMillis();
        if (Double.isNaN(resDouble) || Double.isInfinite(resDouble)) {
            return max;
        }
        Duration res = Duration.ofMillis((long) resDouble);
        if (res.compareTo(max) > 0) {
            return max;
        } else {
            return res;
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

    static BiFunction<Long, Throwable, Duration> maxRetriesWithFixedDelay(long maxRetries, Duration delay) {
        return (i, th) -> i < maxRetries ? delay : null;
    }

    static BiFunction<Long, Throwable, Duration> maxRetriesWithExponentialDelay(long maxRetries, Duration min, Duration max, double factor) {
        return (i, th) -> i < maxRetries ? exponentialBackoff(i, min, max, factor) : null;
    }

    static BiFunction<Long, Throwable, Duration> maxRetriesWithBinaryExponentialDelay(long maxRetries, Duration min, Duration max) {
        return (i, th) -> i < maxRetries ? exponentialBackoff(i, min, max, 2) : null;
    }
    
    static BiFunction<Long, Throwable, Duration> foreverWithFixedDelay(Duration delay) {
        return (i, th) -> delay;
    }

    static BiFunction<Long, Throwable, Duration> foreverWithExponentialDelay(Duration min, Duration max, double factor) {
        return (i, th) -> exponentialBackoff(i, min, max, factor);
    }

    static BiFunction<Long, Throwable, Duration> foreverWithBinaryExponentialDelay(Duration min, Duration max) {
        return (i, th) -> exponentialBackoff(i, min, max, 2);
    }

    
    /**
     * Retry worker, class that actually performs asynchronous retries.
     *
     * @param <T> result type
     */
    class Worker<T> {
        private long currentTry;
        private final Supplier<T> supplier;
        private BiFunction<Long, Throwable, Duration> backoff = maxRetriesWithFixedDelay(10, Duration.ofMillis(100));
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

        /**
         * Specify retry backoff strategy. 
         *  
         * @param backoffFunction bi-function that maps current try number (starting with 0)
         * and caught exception to the duration to wait until next try.
         * @return this to allow method chaining
         */
        public Worker<T> withBackoff(BiFunction<Long, Throwable, Duration> backoffFunction) {
            this.backoff = backoffFunction;
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
                Duration waitDur = backoff.apply(currentTry++, t);
                if (waitDur == null) {
                    result.completeExceptionally(t);
                } else {
                    CompletableFuture
                    .delayedExecutor(waitDur.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(this::tryOnce);
                }
            } catch (Throwable th) {
                result.completeExceptionally(th);
            }
        }
    }
    
}
