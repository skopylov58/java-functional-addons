package com.github.skopylov58.functional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Interface to handle exceptions in a functional way. 
 * 
 * <p>
 * Try can be either success with value of type T or failure with an exception. 
 * In some way Try is similar to {@link Optional} which may have value or not.
 * Use {@link #isSuccess()} and {@link #isFailure()} methods to determine flavor of current try.
 *
 * <p>
 * Try has static factory methods <code>Try.of(...)</code> for producing tries from 
 * {@link ExceptionalSupplier}, {@link ExceptionalRunnable} functional interfaces.
 * 
 * <p>Methods that return Try could be used for method chaining.
 * <pre>
 *   Try.of(...)
 *   .map(...)
 *   .map(...)
 *   .filter(...)
 *   .recover(...)
 *   .recover(...)
 *   .onFailure(...)
 *   .optional()
 *   .get(...)
 * </pre>
 * 
 * <p>
 * Be aware that these Try methods behave differently depending on success or failure.
 * For example {@link #onSuccess(ExceptionalConsumer)} method does not have any effect 
 * in case of failure and {@link #onFailure(Consumer)} does not have any effect for success. 
 * 
 * <p>
 * When Try becomes failure, the only way to get back to success is <code>recover</code> method.
 * Try has a set of <code>recover(...)</code> methods to recover failed try to the success.
 * You can invoke <code>recover</code> many times implementing different recover strategies. 
 * If some <code>recover</code> method succeeds, then remaining <code>recover</code> methods 
 * will not have an action/effect. 
 *
 * @author skopylov@gmail.com
 * 
 * @param <T> type of Try's success result.
 */

public interface Try<T> extends AutoCloseable {

    //------------------
    // Interface methods
    //------------------
    /**
     * Executes action on success, has no action for failure.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    Try<T> onSuccess(ExceptionalConsumer<? super T> consumer);

    /**
     * Executes action on failure, has no action for success.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    Try<T> onFailure(Consumer<Exception> consumer);

    /**
     * Executes action on failure if predicate is true, has no action for success.
     * @param consumer consumer
     * @param predicate predicate to test
     * @return this Try or new failure is consumer throws an exception
     */
    Try<T> onFailure(Consumer<Exception> consumer, Predicate<Exception> predicate);

    /**
     * Checks if it is success try.
     * @return true if it is success.
     */
    boolean isSuccess();

    /**
     * Tries recover failed try with given supplier, has no action for success.
     * @param supplier supplier to recover
     * @return this Try for success, new success or new failure depending on if supplier had thrown exception.
     */
    Try<T> recover(ExceptionalSupplier<T> supplier);

    /**
     * Tries recover failed try with given supplier, has no action for success.
     * @param supplier supplier to recover
     * @param predicate recover attempt happens if predicate returns true.
     * @return this Try for success, new success or new failure depending on if supplier had thrown exception.
     */
    Try<T> recover(ExceptionalSupplier<T> supplier, ExceptionalPredicate<Exception> predicate);

    /**
     * Maps Try of type T to type R 
     * @param <R> new result type
     * @param mapper mapper
     * @return new Try of R type or failure if mapper throws exception/
     */
    <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper); 

    <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper, BiConsumer<T, Exception> errHandler); 

    
    /**
     * Maps Try of type T to type R 
     * @param <R> new result type
     * @param mapper mapper
     * @return new Try of R type or failure if mapper throws exception/
     */
    <R> Try<R> flatMap(ExceptionalFunction<? super T, Try<R>> mapper); 

    
    /**
     * Filters current Try, has no action for failure.
     * @param predicate predicate to test
     * @return this Try if predicate returns true or new fail with {@link NoSuchElementException}
     */
    Try<T> filter(Predicate<T> predicate);
    
    /**
     * Filters current Try, has no action for failure.
     * @param predicate predicate to test
     * @param consumer consumer to notify in case of predicate returns false.
     * @return this Try if predicate returns true or new fail with {@link NoSuchElementException}
     */
    Try<T> filter(Predicate<T> predicate, Consumer<T> consumer);
    
    Stream<T> stream();
    
    Optional<T> optional();
    
    @Override
    void close() ;

    /**
     * Gets Try's value or throws exception 
     * @return value of T in case of success
     * @throws RuntimeException in case of failure 
     */
    T orElseThrow();

    //-----------------------
    //Interface default methods
    //-----------------------

    /**
     * Checks if it is failure try.
     * @return true if failure.
     */
    default boolean isFailure() {return !isSuccess();}
    
    /**
     * Synonym for {@link #orElseThrow()}  
     * @return see {@link #orElseThrow()}
     */
    default T get() {
        return orElseThrow();
    }
    
    /**
     * Behaves like finally block in Java's try/catch/finally.
     * Will executed for both cases - either success or failure.
     * @param runnable runnable to execute
     * @return this Try or new failure if runnable throws exception.
     */
    default Try<T> andFinally(ExceptionalRunnable runnable) {
        try {
            runnable.runWithException();
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Gives access to current Try.
     * @param consumer Try's consumer
     * @return this Try or failure if consumer throws an exception.
     */
    default Try<T> peek(ExceptionalConsumer<Try<T>> consumer) {
        try {
            consumer.acceptWithException(this);
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Convenience method to interrupt current thread if failure cause is {@link InterruptedException}
     * @return this Try
     */
    default Try<T> ifInterrupted() {
        return onFailure(t-> Thread.currentThread().interrupt(), InterruptedException.class::isInstance);
    }

    //----------------------------------
    // Factory methods for producing Try
    //----------------------------------
    
    /**
     * Factory method to produce Try from value of T type.
     * @param <T> result type
     * @param value success value
     * @return Try &lt;T&gt;
     */
    static <T> Try<T> success(T value) {return new Success<>(value);}

    /**
     * Factory method to produce Try from value of Exception.
     * @param <T> result type
     * @param value exception
     * @return Try &lt;T&gt;
     */
    static <T> Try<T> failure(Exception value) {return new Failure<>(value);}
    
    /**
     * Factory method to produce Try from supplier that may throw an exception.
     * <p>
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception.
     * @return Try of T type
     * @throws NullPointerException if supplier returns null.
     */
    static <T> Try<T> of(ExceptionalSupplier<T> supplier) {
        try {
            return success(supplier.getWithException());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Factory method to produce Try from runnable that may throw an exception.
     * @param runnable exceptional runnable
     * @return Try of {@link Void} type
     */
    static Try<Class<Void>> of(ExceptionalRunnable runnable) {
        return of(() -> {
            runnable.runWithException();
            return Void.TYPE;
        });
    }
    
    
    
    
    /**
     * Try's success projection.
     * @author skopylov@goole.com
     *
     * @param <T> result type
     */
    class Success<T> implements Try<T> {
        
        private final T value;
        
        Success(T val) {
            value = val;
        }
        
        @Override
        public <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper) {
          try {
              return success(mapper.applyWithException(value));
          } catch (Exception e) {
              return failure(e);
          }
        }
        
        @Override
        public <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper, BiConsumer<T, Exception> errHandler) {
            try {
                return success(mapper.applyWithException(value));
            } catch (Exception e) {
                errHandler.accept(value, e);
                return failure(e);
            }
        }
        
        @Override
        public <R> Try<R> flatMap(ExceptionalFunction<? super T, Try<R>> mapper) {
            try {
                return mapper.applyWithException(value);
            } catch (Exception e) {
                return failure(e);
            }
        }

        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier) {return this;}
        
        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier, ExceptionalPredicate<Exception> predicate) {
            return this;
        }
        
        @Override
        public Try<T> filter(Predicate<T> pred) {
            return filter(pred, (ExceptionalConsumer<T>) null);
        }

        @Override
        public Try<T> filter(Predicate<T> pred, Consumer<T> consumer) {
            if (!pred.test(value)) {
                if (consumer != null) {
                    consumer.accept(value);
                }
                return failure(new NoSuchElementException());
            } else {
                return this;
            }
        }

        @Override
        public boolean isSuccess() {return true;}
        
        @Override
        public Try<T> onSuccess(ExceptionalConsumer<? super T> consumer) {
            try {
                consumer.acceptWithException(value);
                return this;
            } catch (Exception e) {
                return failure(e);
            }
        }
        
        @Override
        public Try<T> onFailure(Consumer<Exception> consumer) {return this;}
        
        @Override
        public T orElseThrow() {return value;}
        
        @Override
        public Try<T> onFailure(Consumer<Exception> consumer, Predicate<Exception> predicate) {
            return this;
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(value);
        }

        @Override
        public Optional<T> optional() {
            return Optional.ofNullable(value);
        }

        @Override
        public void close() {
            if (value instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) value).close();
                } catch (Exception e) {
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }
    
    /**
     * Try's failure projection.
     * @author skopylov@gmail.com
     *
     * @param <T> result type
     */
    class Failure<T> implements Try<T> {
        
        private final Exception exception;
        
        Failure(Exception e) {
            exception = e;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper) {return (Try<R>) this;}
        
        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier) {
            return recover(supplier, null);
        }
        
        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier, ExceptionalPredicate<Exception> predicate) {
            try {
                if (predicate != null && !predicate.testWithException(exception)) {
                    return this;
                }
                return success(supplier.getWithException());
            } catch (Exception e) {
                return failure(e);
            }            
        }
        
        @Override
        public Try<T> filter(Predicate<T> pred) {return this;}

        @Override
        public boolean isSuccess() {return false;}
        
        @Override
        public Try<T> onSuccess(ExceptionalConsumer<? super T> consumer) {return this;}
        
        @Override
        public Try<T> filter(Predicate<T> pred, Consumer<T> consumer) {return this;}
        
        @Override
        public T orElseThrow() {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <R> Try<R> flatMap(ExceptionalFunction<? super T, Try<R>> mapper) {return (Try<R>) this;}

        @Override
        public Try<T> onFailure(Consumer<Exception> consumer) {
            consumer.accept(exception);
            return this;
        }

        @Override
        public Try<T> onFailure(Consumer<Exception> consumer, Predicate<Exception> predicate) {
            if (predicate.test(exception)) {
                consumer.accept(exception);
            }
            return this;
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public Optional<T> optional() {
            return Optional.empty();
        }

        @Override
        public <R> Try<R> map(ExceptionalFunction<? super T, ? extends R> mapper, BiConsumer<T, Exception> errHandler) {
            return (Try<R>) this;
        }

        @Override
        public void close() {/*nothing to close*/}
        
    }

}
