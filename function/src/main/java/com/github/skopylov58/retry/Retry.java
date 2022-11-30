package com.github.skopylov58.retry;

import java.time.Duration;
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
 * that may throw an exception.
 * <p>
 * Retry.of(...) factory methods create {@link Worker} from
 * {@link CheckedSupplier}.
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
    
    /** Supplier that may throw an exception. */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
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
            } catch (Exception e) {
                sneakyThrow(e);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
    
    /**
     * Retry worker, class that actually performs asynchronous retries.
     *
     * @param <T> result type
     */
    class Worker<T> {
        private long currentTry;
        private final Supplier<T> supplier;
        private Executor executor = ForkJoinPool.commonPool();
        private final CompletableFuture<T> result = new CompletableFuture<>();
        
        //Functional stuff
        private Predicate<Long> maxTries         = i -> true; //Forever by default
        private Predicate<Throwable> onThrowable = t -> true; //Handle all exceptions by default 
        private Function<Long, Duration> backoff = i -> Duration.ofSeconds(1); //1 sec by default
        private BiConsumer<Long, Throwable> onError = null;
        private Predicate<T> retryOnValue = null;
        
        /**
         * Constructor.
         * @param supplier
         */
        private Worker(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * Specifies executor to perform retries.
         * <p>Default - ForkJoinPool.commonPool()
         * @param executor executor to use for retry.
         * @return this Worker
         */
        public Worker<T> withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }
        
        /**
         * Specifies backoff retry strategy with fixed delay.
         * <p>Default - 1 second.
         * @param delay delay between tries. 
         * @return this Worker
         */
        public Worker<T> withFixedDelay(Duration delay) {
            backoff = i -> delay;
            return this;
        }

        /**
         * With backoff function you can implement different retry delay strategies
         * - linear, exponential, fibonacci, etc.
         * <p>Default - fixed delay 1 second.
         * @param backoff function that maps current try number (starting with 0) to delay
         * @return this Worker
         */
        public Worker<T> withBackoff(Function<Long, Duration> backoff) {
            this.backoff = backoff;
            return this;
        }

        /**
         * Error listener.
         * <p>Default - no error listener.
         * @param onError bi consumer with try number and error
         * @return this Worker
         */
        public Worker<T> onError(BiConsumer<Long, Throwable> onError) {
            this.onError = onError;
            return this;
        }

        /**
         * Specifies which exception(s) to retry. If predicate return true then worker will retry 
         * this exception, otherwise complete future exceptionally.
         * <p>Default - retry on any Exception.
         * @param pred predicate to test.
         * @return this Worker
         */
        public Worker<T> retryOnException(Predicate<Throwable> pred) {
            this.onThrowable = pred;
            return this;
        }

        /**
         * Allows retry on some specific return values, like "Service not available", null values,  etc.
         * <p>Default - no retry on any return value.
         * 
         * @param pred predicate to test return values. if predicate returns true then
         * worker will retry operation.
         * @return this Worker
         */
        public Worker<T> retryOnValue(Predicate<T> pred) {
            this.retryOnValue  = pred;
            return this;
        }

        /**
         * Starts retry forever operation.
         * @return CompletableFuture
         */
        public CompletableFuture<T> retry() {
            tryOnce();
            return result;
        }

        /**
         * Retry with maximum number of tries
         * @param maxRetries maximum number of tries
         * @return CompletableFuture
         */
        public CompletableFuture<T> retry(long maxRetries) {
            maxTries = i -> i < maxRetries;
            return retry();
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
                if (retryOnValue != null && retryOnValue.test(res)) {
                    handleError(new IllegalStateException("Retry on value: " + res));
                } else {
                    result.complete(res);
                }
            }
        }

        private void handleError(Throwable t) {
            try {
                if (onError != null) {
                    onError.accept(currentTry, t);
                }
                if (!maxTries.test(currentTry)) {
                    result.completeExceptionally(t);
                    return;
                }
                if (!onThrowable.test(t)) {
                    result.completeExceptionally(t);
                    return;
                }
                Duration delay = backoff.apply(currentTry++);
                if (delay == null) {
                    result.completeExceptionally(t);
                } else {
                    CompletableFuture
                    .delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(this::tryOnce);
                }
            } catch (Throwable th) {
                th.initCause(t);
                result.completeExceptionally(th);
            }
        }
    }
    
}
