package com.github.skopylov58.retry;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.skopylov58.functional.ExceptionalRunnable;
import com.github.skopylov58.functional.ExceptionalSupplier;

/**
 * Interface to perform asynchronous retry operations on supplier
 * or runnable that may throw an exception.
 * <p>
 * Retry.of(...) factory methods create {@link Builder} from 
 * {@link Supplier} or {@link Runnable}
 * <p>
 * Sample usage
 * <pre>
 *      CompletableFuture&lt;Connection&gt; futureConnection = 
 *      Retry.of(() -&gt; DriverManager.getConnection("jdbc:mysql:a:b"))
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
    /** Retry error handler. */
    interface ErrorHandler {
         /** Handles errors during retry process.
         * @param currentTry current try
         * @param maxTries maximum number of tries
         * @param exception exception occurred during current try  
         */
        void handle(long currentTry, long maxTries, Throwable exception);
    }
    
    /**
     * Factory method to creates {@link Builder} for supplier.
     * @param <T> supplier's result type
     * @param supplier supplier
     * @return {@link Builder}
     */
    static <T> Builder<T> of(Supplier<T> supplier) {return new Builder<>(supplier);}

    /**
     * Factory method to creates {@link Builder} for the exceptional supplier.
     * @param <T> supplier's result type
     * @param supplier exceptional supplier
     * @return {@link Builder}
     */
    static <T> Builder<T> of(ExceptionalSupplier<T> supplier) {
        return of (ExceptionalSupplier.uncheck(supplier));
    }

    /**
     * Factory method to creates {@link Builder} for runnable.
     * @param runnable runnable
     * @return {@link Builder}
     */
    static Builder<Void> of(Runnable runnable) {
        return new Builder<>(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Factory method to creates {@link Builder} for exceptional runnable.
     * @param runnable exceptional runnable
     * @return {@link Builder}
     */
    static Builder<Void> of(ExceptionalRunnable runnable) {
        return of(ExceptionalRunnable.uncheck(runnable));
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
    class Builder<T> {
        private static final int DEFAULT_MAX_TRIES = 10;
        private static final int DEFAULT_DELAY = 1;
        private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
        
        private final Supplier<T> supplier;
        private long maxTries = DEFAULT_MAX_TRIES;
        private long delay = DEFAULT_DELAY;
        private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
        private ErrorHandler errorHandler;
        private Executor executor;
        
        Builder(Supplier<T> supplier) {this.supplier = supplier;}
        
        /**
         * Sets max number of tries. Default - 10.
         * <p>
         * Zero or negative value means infinite number of tries. 
         * Use {@link #forever()} for this purpose.
         * 
         * @param maxTries max number of tries
         * @return {@link Builder}
         */
        public Builder<T> maxTries(long maxTries) {
            this.maxTries = maxTries; 
            return this;
        }

        /**
         * Sets infinite number of tries.
         * @return {@link Builder}
         */
        public Builder<T> forever() {
            this.maxTries = 0;
            return this;
        }

        /**
         * Sets retry delay. 
         * @param delay retry delay. Default - 1 
         * @param timeUnit delay time unit. Default - seconds
         * @return {@link Builder}
         */
        public Builder<T> delay(long delay, TimeUnit timeUnit) {
            this.delay = delay;
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Sets error handler. Default - no error handler.
         * @param errorHandler
         * @return {@link Builder} builder
         */
        public Builder<T> withErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets executor for retry process.
         * @param executor executor. Default - {@link ForkJoinPool#commonPool()}
         * @return builder
         */
        public Builder<T> withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Non blocking final builder operation.
         * @return immediately returns {@link CompletableFuture}
         */
        public CompletableFuture<T> retry() {
            Option opt = new Option(maxTries, delay, timeUnit);
            return new Worker<T>(supplier, opt, errorHandler, executor).retry();
        }
    }
    
    /**
     * Class that actually performs asynchronous retries.
     *
     * @param <T> result type
     */
    class Worker<T> {
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
        private Worker(Supplier<T> supplier, Option opts, ErrorHandler errorHandler, Executor executor) {
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
                CompletableFuture.supplyAsync(supplier).whenComplete(this::whenComplete);
            } else {
                CompletableFuture.supplyAsync(supplier, executor).whenComplete(this::whenComplete);
            }
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
