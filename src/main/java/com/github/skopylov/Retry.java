package com.github.skopylov;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Interface to perform asynchronous retry operations on supplier
 * and runnable that may throw an exception.
 * <p>
 * Retry.of(...) factory methods create {@link RetryBuilder} from 
 * {@link ExceptionalSupplier} or {@link ExceptionalRunnable}
 * <p>
 * Sample usage
 * <pre>
 *      CompletableFuture<Connection> futureConnection = 
 *      Retry.of(() -> DriverManager.getConnection("jdbc:mysql:a:b"))
 *      .maxTries(100)
 *      .delay(1, TimeUnit.SECONDS)
 *      .withErrorHandler(...)
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
     * Handles errors happening during retry process.
     *
     */
    @FunctionalInterface
    interface ErrorHandler {
        /*
         * @param currentTry current try
         * @param maxTries maximum number of tries
         * @param exception exception occurred during current try  
         */
        void handle(long currentTry, long maxTries, Throwable exception);
    }
    
    /**
     * Factory method to creates {@link RetryBuilder} for the exceptional supplier.
     * @param <T> supplier's result type
     * @param supplier exceptional supplier
     * @return {@link RetryBuilder}
     */
    static <T> RetryBuilder<T> of(Supplier<T> supplier) {return new RetryBuilder<>(supplier);}
    
    
    /**
     * Factory method to creates {@link RetryBuilder} for the exceptional runnable.
     * @param runnable exceptional runnable
     * @return {@link RetryBuilder}
     */
    static RetryBuilder<Class<Void>> of(Runnable runnable) {
        return of(() -> {runnable.run(); return Void.TYPE;});
    }

    /**
     * Retry options in terms of maximum numbers of tries, delay interval and time units.
     */
    class Option {
        final long maxTries;
        final long delay;
        final TimeUnit timeUnit;
        
        Option(long maxTries, long delay, TimeUnit timeUnit) {
            this.maxTries = maxTries;
            this.delay = delay;
            this.timeUnit = timeUnit;
        }
    }

    /**
     * Retry builder to configure retry process with maxTries, delay, error handler, etc.
     *
     */
    class RetryBuilder<T> {
        private static final int DEFAULT_MAX_TRIES = 10;
        private static final int DEFAULT_DELAY = 1;
        private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
        
        private final Supplier<T> supplier;
        private long maxTries = DEFAULT_MAX_TRIES;
        private long delay = DEFAULT_DELAY;
        private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
        private ErrorHandler errorHandler;
        private Executor executor;
        
        RetryBuilder(Supplier<T> supplier) {this.supplier = supplier;}
        
        /**
         * Sets max number of tries. Default - 10.
         * <p>
         * Zero or negative value means infinite number of tries. 
         * Use {@link #forever()} for this purpose.
         * 
         * @param maxTries max number of tries
         * @return {@link RetryBuilder}
         */
        public RetryBuilder<T> maxTries(long maxTries) {
            this.maxTries = maxTries; 
            return this;
        }

        /**
         * Sets infinite number of tries.
         * @return {@link RetryBuilder}
         */
        public RetryBuilder<T> forever() {
            this.maxTries = 0;
            return this;
        }

        /**
         * Sets retry delay. 
         * @param delay retry delay. Default - 1 
         * @param timeUnit delay time unit. Default - seconds
         * @return {@link RetryBuilder}
         */
        public RetryBuilder<T> delay(long delay, TimeUnit timeUnit) {
            this.delay = delay;
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Sets error handler. Default - no error handler.
         * @param errorHandler
         * @return {@link RetryBuilder}
         */
        public RetryBuilder<T> withErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets executor for retry process.
         * @param executor executor. Default - {@link ForkJoinPool#commonPool()}
         * @return
         */
        public RetryBuilder<T> withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Non blocking final builder operation.
         * @return immediately returns {@link CompletableFuture}
         */
        public CompletableFuture<T> retry() {
            Option opt = new Option(maxTries, delay, timeUnit);
            return new Retrier<T>(supplier, opt, errorHandler, executor).retry();
        }
    }
    
    /**
     * Class that actually performs asynchronous retries.
     *
     * @param <T> result type
     */
    class Retrier<T> {
        private long currentTry;
        
        private final Option opts;
        private final Supplier<T> supplier;
        private final ErrorHandler errorHandler;
        private final Executor executor;
        private final CompletableFuture<T> result = new CompletableFuture<>();

        /**
         * Constructor.
         * @param supplier
         * @param opts
         * @param errorHandler
         */
        private Retrier(Supplier<T> supplier, Option opts, ErrorHandler errorHandler, Executor executor) {
            this.supplier = supplier;
            this.opts = opts;
            this.errorHandler = errorHandler;
            this.executor = executor;
        }

        private CompletableFuture<T> retry() {
            tryOnce();
            return result;
        }
        
        private void tryOnce() {
            if (executor == null) {
                CompletableFuture.supplyAsync(supplier).handle(this::handle);
            } else {
                CompletableFuture.supplyAsync(supplier, executor).handle(this::handle);
            }
        }

        private T handle(T res, Throwable t) {
            if (t != null) {
                handleError(t);
            } else {
                result.complete(res);
            }
            return res;
        }
        
        private void handleError(Throwable t) {
            Objects.requireNonNull(t);
            ++currentTry;
            if (errorHandler != null) {
                try {
                    errorHandler.handle(currentTry, opts.maxTries, t);
                } catch (Throwable th) {
                    //ignore
                }
            }
            if (currentTry < opts.maxTries || opts.maxTries <= 0) {
                CompletableFuture.delayedExecutor(opts.delay, opts.timeUnit)
                .execute(this::tryOnce);
            } else {
                result.completeExceptionally(t);
            }
        }
    }
}
