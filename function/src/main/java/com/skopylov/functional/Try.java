package com.skopylov.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Interface to handle exceptions in a functional way. 
 * 
 * <p>
 * Try can be either success with value of type T or failure with an exception. 
 * In some way Try is similar to {@link Optional} which may have value or not.
 * Use {@link #isSuccess()} and {@link #isFailure()} methods to determine flavor of current try.
 *
 * From the technical perspective, Try&lt;T&gt; is specialization of {@link Either} &lt;T, Exception&gt;
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
 *   .logException(...)
 *   .onSuccess(...)
 *   .getOrDefault(...)
 * </pre>
 * 
 * <p>
 * Be aware that these Try methods behave differently depending on success or failure.
 * For example {@link #onSuccess(ExceptionalConsumer)} method does not have any effect 
 * in case of failure and {@link #onFailure(ExceptionalConsumer)} does not have any effect for success. 
 * 
 * <p>
 * When Try becomes failure, the only way to get back to success is <code>recover</code> method.
 * Try has a set of <code>recover(...)</code> methods to recover failed try to the success.
 * You can invoke <code>recover</code> many times implementing different recover strategies. 
 * If some <code>recover</code> method succeeds, then remaining <code>recover</code> methods 
 * will not have an action/effect. 
 *
 * <pre>
 *   Try.of (() -&gt; new FileInputStream("path/to/file")).autoClose()
 *   .map(...)
 *   .getOrThrow() // this will close FileInputStream
 *   
 * </pre>
 * 
 * @author skopylov@gmail.com
 * 
 * @param <T> type of Try's success result.
 */

public interface Try<T> extends Either<T, Exception>, AutoCloseable {

    ThreadLocal<List<AutoCloseable>> resources = ThreadLocal.withInitial(ArrayList::new);  

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
     * Factory method to produce Try from supplier that may throw exception.
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception.
     * @return Try of T type
     */
    static <T> Try<T> of(ExceptionalSupplier<T> supplier) {
        try {
            return success(supplier.getWithException());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Factory method to produce Try from supplier that may produce
     * some valuable result of T, return valid null or throw exception.
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception.
     * @return Try of T type
     */
    static <T> Try<Optional<T>> ofNullable(ExceptionalSupplier<T> supplier) {
        return of(() -> Optional.ofNullable(supplier.getWithException()));
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
    class Success<T> extends Either.Right<T, Exception> implements Try<T> {
        Success(T val) {super(val);}
        
        @Override
        public <R> Try<R> map(ExceptionalFunction<T, R> mapper) {
          try {
              return success(mapper.applyWithException(right));
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
        public Try<T> filter(ExceptionalPredicate<T> pred) {
            return filter(pred, (ExceptionalConsumer<T>) null);
        }

        @Override
        public Try<T> filter(ExceptionalPredicate<T> pred, ExceptionalConsumer<T> consumer) {
            try {
                if (!pred.testWithException(right)) {
                    if (consumer != null) {
                        consumer.acceptWithException(right);
                    }
                    return failure(new NoSuchElementException());
                } else {
                    return this;
                }
            } catch (Exception e) {
                return failure(e);
            }
        }

        @Override
        public boolean isSuccess() {return true;}
        
        @Override
        public Try<T> onSuccess(ExceptionalConsumer<T> consumer) {
            try {
                consumer.acceptWithException(right);
                return this;
            } catch (Exception e) {
                return failure(e);
            }
        }
        
        @Override
        public Try<T> onFailure(ExceptionalConsumer<Exception> consumer) {return this;}
        
        @Override
        public T orElseThrow() {return right;}
        
        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> s) throws X {return right;}

        @Override
        public Try<T> autoClose() {
            if (right instanceof AutoCloseable) {
                resources.get().add((AutoCloseable) right);
                return this;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public Optional<Exception> getFailureCause() {
            return Optional.empty();
        }
    }
    
    /**
     * Try's failure projection.
     * @author skopylov@gmail.com
     *
     * @param <T> result type
     */
    class Failure<T> extends Either.Left<T, Exception> implements Try<T> {
        Failure(Exception e) {super(e);}
        
        @SuppressWarnings("unchecked")
        @Override
        public <R> Try<R> map(ExceptionalFunction<T, R> mapper) {return (Try<R>) this;}
        
        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier) {
            return recover(supplier, null);
        }
        
        @Override
        public Try<T> recover(ExceptionalSupplier<T> supplier, ExceptionalPredicate<Exception> predicate) {
            try {
                if (predicate != null && !predicate.testWithException(left)) {
                    return this;
                }
                return success(supplier.getWithException());
            } catch (Exception e) {
                return failure(e);
            }            
        }
        
        @Override
        public Try<T> filter(ExceptionalPredicate<T> pred) {return this;}

        @Override
        public boolean isSuccess() {return false;}
        
        @Override
        public Try<T> onSuccess(ExceptionalConsumer<T> consumer) {return this;}
        
        @Override
        public Try<T> onFailure(ExceptionalConsumer<Exception> consumer) {
            try {
                consumer.acceptWithException(left);
                return this;
            } catch (Exception e) {
                return failure(e);
            }
        }

        @Override
        public Try<T> filter(ExceptionalPredicate<T> pred, ExceptionalConsumer<T> consumer) {return this;}
        
        @Override
        public T orElseThrow() {
            if (left instanceof RuntimeException) {
                throw (RuntimeException) left;
            } else {
                throw new RuntimeException(left);
            }
        }
        
        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> s) throws X {
            X x = s.get();
            x.initCause(left);
            throw x;
        }

        @Override
        public Try<T> autoClose() {return this;}

        @Override
        public Optional<Exception> getFailureCause() {
            return Optional.of(left);
        }
        
    }

    /**
     * Checks if it is success try.
     * @return true if it is success.
     */
    boolean isSuccess();
    
    /**
     * Checks if it is failure try.
     * @return true if failure.
     */
    default boolean isFailure() {return !isSuccess();}
    
    /**
     * Cast Try&lt;T&gt; to Try &lt;R&gt;
     * @param <R> result type
     * @param c class cast to
     * @return Try &lt;R&gt;
     */
    @SuppressWarnings("unchecked")
    default <R> Try<R> cast(Class<R> c) {
        return (Try<R>) this;
    }
    
    /**
     * Closes resources marked by link autoClose()
     */
    default void close() {
        closeResources();
    }
    
    
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
     * Closes resources marked by {@link #autoClose()},
     * works for both cases - either success or failure.
     * @return this Try
     */
    default Try<T> closeResources() {
        List<AutoCloseable> list = resources.get();
        if (list == null || list.isEmpty()) {
            return this;
        }
        list.forEach(c -> {
            try {
                c.close();
            } catch (Exception e) {
                //silently
            }
        });
        list.clear();
        resources.remove();
        return this;
    }

    /**
     * Gets Try's value or throws exception 
     * @return value of T in case of success
     * @throws RuntimeException in case of failure 
     */
    T orElseThrow();

    /**
     * Marks this Try to be closed by {@link #close()} or {@link #closeResources()} methods.
     * <p>
     * Note that Try's value should be {@link AutoCloseable} instance.
     * @return this Try
     * @throws IllegalArgumentException if Try's value is not instance {@link AutoCloseable} 
     */
    Try<T> autoClose();
    
    /**
     * Gets the failure cause.
     * @return Optional of Exception
     */
    Optional<Exception> getFailureCause();
    
    /**
     * @param <X> throws type
     * @param supplier exception supplier
     * @return value of type T in case of success.
     * @throws X it this is failure
     */
    <X extends Throwable> T orElseThrow(Supplier<? extends X> supplier) throws X;
    
    /**
     * Executes action on success, has no action for failure.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    Try<T> onSuccess(ExceptionalConsumer<T> consumer);

    /**
     * Executes action on failure, has no action for success.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    Try<T> onFailure(ExceptionalConsumer<Exception> consumer);

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
     * Maps one Try of type T to type R 
     * @param <R> new result type
     * @param mapper mapper
     * @return new Try of R type or failure if mapper throws exception/
     */
    <R> Try<R> map(ExceptionalFunction<T, R> mapper); 
    
    /**
     * Filters current Try, has no action for failure.
     * @param predicate predicate to test
     * @return this Try if predicate returns true or new fail with {@link NoSuchElementException}
     */
    Try<T> filter(ExceptionalPredicate<T> predicate);
    
    /**
     * Filters current Try, has no action for failure.
     * @param predicate predicate to test
     * @param consumer consumer to notify in case of predicate returns false.
     * @return this Try if predicate returns true or new fail with {@link NoSuchElementException}
     */
    Try<T> filter(ExceptionalPredicate<T> predicate, ExceptionalConsumer<T> consumer);
    
}
