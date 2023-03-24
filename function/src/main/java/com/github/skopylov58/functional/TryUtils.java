package com.github.skopylov58.functional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.skopylov58.functional.Try.CheckedRunnable;

/**
 * Collection of higher-order functions to handle exceptions in functional style.
 * 
 * @author skopylov@gmail.com
 *
 */
public interface TryUtils {

    /**
     * Function that may throw an exception.
     */
    @FunctionalInterface
    interface CheckedFunction<T, R > {
        R apply(T t) throws Exception;
    }

    /** Supplier that may throw an exception. */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /** Try Result for Java 8+ */
    public static class ResultJava8<T> {
        private final Object value;
        public ResultJava8(T t) {value = t;}
        public ResultJava8(Exception e ) {value = e;}
        public boolean failed () {return value instanceof Exception;} 
        public T result() {return (T) value;}
        public Exception cause() {return (Exception) value;}
    }

    /** Try Result for Java 14+ */
    record Result<T>(T result, Exception exception) {
        public boolean failed() {return exception != null;}
        public Stream<T> stream() {return failed() ? Stream.empty() : Stream.of(result);}
    } 

    /**
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Result<R>}
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function {@code T=>R} that may throw checked exception
     * @return total function {@code T=>Result<R>}
     */
    static <T, R> Function<T, Result<R>> toResult(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return new Result<>(func.apply(param), null);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                return new Result<>(null, e);
            }
        };
    }
    
    /**
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Optional<R>}
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function {@code T=>R} that may throw checked exception
     * @return total function {@code T=>Optional<R>}
     */
    static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return Optional.ofNullable(func.apply(param));
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    /**
     * Higher-order function to convert partial supplier {@code ()=>T} to total supplier {@code ()=>Optional<T>}
     * @param <T> supplier result type
     * @param supplier {@code ()=>T} that may throw an exception
     * @return total supplier {@code ()=>Optional<T>}
     */
    static <T> Supplier<Optional<T>> toOptional(CheckedSupplier<T> supplier) {
        return () -> {
            try {
                return Optional.ofNullable(supplier.get());
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
    /**
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Either<R, Exception>}
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function {@code T=>R} that may throw an exception
     * @return total function {@code T=>Either<R, Exception>}
     */
    static <T, R> Function<T, Either<R, Exception>> toEither(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return Either.right(func.apply(param));
            } catch (Exception e) {
                return Either.left(e);
            }
        };
    }

    static <T, R> Function<T, ResultJava8<R>> toResultJava8(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return new ResultJava8<>(func.apply(param));
            } catch (Exception e) {
                return new ResultJava8<>(e);
            }
        };
    }

    static <T, R> Function<T, CompletableFuture<R>> toFuture(CheckedFunction<T, R> func) {
        return param -> {
            try {
                return CompletableFuture.completedFuture(func.apply(param));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    static <T> Supplier<T> toSupplier(CheckedSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                sneakyThrow(e);
                return null;  // we never will get here
            }
        };
    }
    
    static Duration measure(Runnable runnable) {
        Instant start = Instant.now();
        runnable.run();
        Instant end = Instant.now();
        return Duration.between(start, end);
    }
}
