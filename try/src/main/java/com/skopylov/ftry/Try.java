package com.skopylov.ftry;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.skopylov.functional.Either;
import com.skopylov.functional.ExceptionalConsumer;
import com.skopylov.functional.ExceptionalFunction;
import com.skopylov.functional.ExceptionalPredicate;
import com.skopylov.functional.ExceptionalRunnable;
import com.skopylov.functional.ExceptionalSupplier;
import com.skopylov.functional.TryException;

/**
 * Class to handle exceptions in a functional way. 
 * 
 * <p>
 * Try can be either success with value of type T or failure with an exception. 
 * In some way Try is similar to {@link Optional} which may have value or not.
 * Use {@link #isSucces()} and {@link #isFailure()} methods to determine current try.
 * 
 * <p>
 * Try has static factory methods <code>Try.of(...)</code> for producing tries from 
 * {@link ExceptionalSupplier}, {@link ExceptionalRunnable} functional interfaces
 * and {@link CompletableFuture}.
 * 
 * <p>Methods that return Try could be used for method chaining.
 * <pre>
 *   Try.of(...)
 *   .map(...)
 *   .map(...)
 *   .filter(...)
 *   .recover(...)
 *   .recover(...)
 *   .logException(...)
 *   .onSuccess(...)
 *   .getOrDefault(...)
 * </pre>
 * 
 * <p>
 * Be aware that these Try methods behave differently depending on success or failure.
 * For example {@link #onSuccess(ExceptionalConsumer)} method does not have any effect 
 * in case of failure and {@link #logException()} does not have any effect for success. 
 * 
 * <p>
 * When Try becomes failure, the only way to get back to success is <code>recover</code> method.
 * Try has a set of <code>recover(...)</code> methods to recover failed try to the success.
 * You can invoke <code>recover</code> many times implementing different recover strategies. 
 * If some <code>recover</code> method succeeds, then remaining <code>recover</code> methods 
 * will not have an action/effect. 
 *
 * <p>
 * Methods that do not return Try considered as final in the chaining.
 * These methods are {@link #get()}, {@link #getOrDefault(Object)},
 * {@link #getOrThrow()}, {@link #optional()}, {@link #stream()}
 * These methods will close {@link AutoCloseable} resources marked with {@link #autoClose()} method.
 * 
 * <pre>
 *   Try.of (() -> new FileInputStream("path/to/file")).autoClose()
 *   .map(...)
 *   .getOrThrow() // <- this will close FileInputStream
 *   
 * </pre>
 * 
 * @author skopylov@gmail.com
 * 
 * @param <T> type of Try's success result.
 */

public class Try<T> extends TryAutoCloseable {
    private static final Logger LOGGER = System.getLogger(Try.class.getName());
    
    private final Either<T, Exception> either;
    
    private Try(T t) {either = Either.right(t);}
    private Try(Exception e) {
        closeResources();
        either = Either.left(e);
    }
    

    /**
     * Gets Try result. To make predictable result, use {@link #isSucces()} before using this method.
     * @return result of T in case of Success or throws an exception in case of Failure.
     * @throws original runtime exception or TryException which wraps original exception in case of failure.
     */
    public T get() {
        closeResources();
        if (either.isRight()) {
            return either.getRight();
        } else {
            Exception exception = either.getLeft();
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new TryException(exception);
            }
        }
    }
    
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> s) throws X {
        if (either.isRight()) {
            return get();
        } else {
            throw s.get();
        }
    }
    
    /**
     * It is just synonym for {@link #get()} 
     * @return
     */
    public T getOrThrow() {
        return get();
    }
    
    /**
     * Gets Try result.
     * @param defaultValue value to return in case of failure.
     * @return value of T. 
     */
    public T getOrDefault(T defaultValue) {
        return either.isRight() ? get() : defaultValue;  
    }
    
    /**
     * Converts this Try to Optional
     * @return Optional of T
     */
    public Optional<T> optional() {
        return either.isRight() ? Optional.of(get()) : Optional.empty(); 
    }
    
    /**
     * Converts this Try to stream.
     * @return in case of success returns one element T stream, in case of failure returns empty stream
     */
    public Stream<T> stream() {
        return either.isRight() ? Stream.of(get()) : Stream.empty();
    }

    public boolean isSuccess() {
        return either.isRight();
    }

    public boolean isFailure() {
        return either.isLeft();
    }
    
    @SuppressWarnings("unchecked")
    public <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper) {
        if (isSuccess()) {
            try {
                return success(mapper.applyWithException(either.getRight()));
            } catch (Exception e) {
                return failure(e);
            }
        }
        return (Try<R>) this;
    }
    
    public Try<T> filter(ExceptionalPredicate<? super T> predicate) {
        if (isSuccess()) {
            try  {
                return predicate.testWithException(either.getRight()) ? this : failure(new NoSuchElementException());
            } catch (Exception e) {
                return failure(e);
            }
        }
        return this;
    }

    public Try<T> filter(ExceptionalPredicate<? super T> predicate, ExceptionalConsumer<? super T> cons) {
        if (isSuccess()) {
            try  {
                T right = either.getRight();
                boolean tested = predicate.testWithException(right);
                if (!tested) {
                    cons.accept(right);
                }
                return tested ? this : failure(new NoSuchElementException());
            } catch (Exception e) {
                return failure(e);
            }
        }
        return this;
    }

    
    public Try<T> onFailure(ExceptionalConsumer<Exception> cons) {
        if (either.isLeft()) {
            try {
                cons.acceptWithException(either.getLeft());
            } catch (Exception e) {
                return failure(e);
            }
        }
        return this;
    }

    public Try<T> onFailure(Class<? extends Exception> target, ExceptionalConsumer<Exception> c) {
        if (either.isLeft()) {
            Exception left = either.getLeft();
            if (target.isAssignableFrom(left.getClass())) {
                try {
                    c.acceptWithException(left);
                    return this;
                } catch (Exception e) {
                    return failure(e);
                }
            }
        }
        return this;
    }
    
    public Optional<Exception> getFailureCause() {
        if (either.isRight()) {
            return Optional.empty();
        } else {
            return Optional.of(either.getLeft());
        }
    }
    
    public Try<T> onSuccess(ExceptionalConsumer<? super T> cons) {
        if (either.isRight()) {
            try {
                cons.acceptWithException(either.getRight());
            } catch (Exception e) {
                return failure(e);
            }
        }
        return this;
    }

    public <X extends Throwable> Try<T> throwing(UnaryOperator<X> op) throws X {
        if (isFailure()) {
            throw op.apply((X) either.getLeft());
        }
        return this;
    }

    public Try<T> throwing() {
        if (isFailure()) {
            Exception left = either.getLeft();
            if (left instanceof RuntimeException) {
                throw (RuntimeException) left;
            } else {
                throw new RuntimeException(left);
            }
        }
        return this;
    }

    
    
    /**
     * This is just for debugging purposes.
     * @param consumer of Try
     * @return this or new Failure if consumer throws an exception.
     */
    public Try<T> peek(ExceptionalConsumer<Try<T>> consumer) {
        try {
            consumer.acceptWithException(this);
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }
    /**
     * Performs finally operation for both cases - Success or Failure.
     * @param runnable which may throw exception
     * @return this or new Failure if runnable throws an exception.
     */
    public Try<T> andFinally(ExceptionalRunnable runnable) {
        try {
            runnable.runWithException();
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }

    public Try<T> recover(T t) {
        return isFailure() ? success(t) : this; 
    }
    
    public Try<T> recover(ExceptionalSupplier<? extends T> supplier) {
        if (isFailure()) {
            try {
                return recover(supplier.getWithException());
            } catch (Exception e) {
                return failure(e);
            }
        }
        return this;
    }

    public Try<T> recover(T t, Class<? extends Exception> target) {
        if (isFailure()) {
            return target.isAssignableFrom(either.getLeft().getClass()) ? success(t) : this;
        }
        return this;
    }

    public Try<T> recover(ExceptionalSupplier<? extends T> supplier, Class<? extends Exception> target) {
        if (isFailure()) {
            if (target.isAssignableFrom(either.getLeft().getClass())) {
                try {
                    return success(supplier.getWithException());
                } catch (Exception e) {
                    return failure(e);
                }
            }
        }
        return this;
    }    
    
    @SuppressWarnings("unchecked")
    public Try<Class<Void>> recover(ExceptionalRunnable runnable) {
        if (isFailure()) {
            try {
                runnable.runWithException();
                return success(Void.TYPE);
            } catch (Exception e) {
                return failure(e);
            }
        }
        return (Try<Class<Void>>) this;
    }

    public Try<T> logException(Level level) {
        if (isFailure()) {
            Exception exception = either.getLeft();
            LOGGER.log(level, exception.getMessage(), exception);
        }
        return this;
    }

    public Try<T> logException() {
        return logException(Level.INFO);
    }

    public Try<T> autoClose() {
        if (isSuccess()) {
            T right = either.getRight();
            if (right instanceof AutoCloseable) {
                addResource((AutoCloseable) right);
            } else {
                throw new AssertionError(right.getClass().getName() + " must be AutoCloseable");
            }
        }
        return this;
    }
    
    //-------------------------------------
    //Factory methods to produce Try
    //-------------------------------------
    
    public static <T> Try<T> success(T t) {return new Try<>(t);}
    public static <T> Try<T> failure(Exception e) {return new Try<>(e);}
    
    /**
     * Factory method to produce Try from supplier that may throw exception.
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception.
     * @return Try of T type
     */
    public static <T> Try<T> of(ExceptionalSupplier<? extends T> supplier) {
        try {
            return success(supplier.getWithException());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Factory method to produce Try from supplier that may throw exception or may return null.
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception or may return null.
     * @return {@literal Try<Optional<T>>}
     */
    public static <T> Try<Optional<T>> ofNullable(ExceptionalSupplier<? extends T> supplier) {
        try {
            return success(Optional.ofNullable(supplier.getWithException()));
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    
    /**
     * Factory method to produce Try<Void> from runnable which may throw exception.
     * @param runnable which may throw exception. 
     * @return Try of Void type
     */
    public static Try<Class<Void>> of(ExceptionalRunnable runnable) {
        try {
            runnable.runWithException();
            return success(Void.TYPE);
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Factory method to produce Try<T> from future.
     * @param future future of T 
     * @return Try of T type
     */
    public static <T> Try<T> of(CompletableFuture<T> future) {
        try {
            return success(future.get());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return failure(ie);
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    public <R> Try<R> cast(Class<R> c) {
        return (Try<R>) this;
    }
}
