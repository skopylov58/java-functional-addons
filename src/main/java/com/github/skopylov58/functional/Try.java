package com.github.skopylov58.functional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
 * {@link CheckedSupplier}, {@link CheckedRunnable} functional interfaces.
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
 * For example {@link #onSuccess(CheckedConsumer)} method does not have any effect 
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
    
    
    @FunctionalInterface
    interface CheckedSupplier<T> {T get() throws Exception;}

    @FunctionalInterface
    interface CheckedConsumer<T> {void accept(T t) throws Exception;}
    
    @FunctionalInterface
    interface CheckedFunction<T, R> {R apply(T t) throws Exception;}
    
    @FunctionalInterface
    interface CheckedRunnable {void run() throws Exception;}

    
    //------------------
    // Interface methods
    //------------------
    /**
     * Executes action on success, has no action for failure.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    @SuppressWarnings("unchecked")
    default Try<T> onSuccess(CheckedConsumer<? super T> consumer) {
        return map(toFunction(consumer));
    };

    /**
     * Executes action on failure, has no action for success.
     * @param consumer consumer
     * @return this Try or new failure is consumer throws an exception
     */
    default Try<T> onFailure(Consumer<Exception> consumer) {
        return fold(__ -> this, e -> {consumer.accept(e); return this;});
    }

    /**
     * Checks if it is success try.
     * @return true if it is success.
     */
    default boolean isSuccess() {
        return fold(__ -> true, __ -> false);
    };

    /**
     * Tries recover failed try with given supplier, has no action for success.
     * @param supplier supplier to recover
     * @return this Try for success, new success or new failure depending on if supplier had thrown exception.
     */
    @SuppressWarnings("unchecked")
    default Try<T> recover(CheckedSupplier<? extends T> supplier) {
        return (Try<T>) fold(__ -> this, e -> catching(toFunction(supplier)).apply(e));
    }

//    default Try<T> recover(Supplier<? extends T> supl) {
//        return (Try<T>) fold(__ -> this, e -> success(supl.get()));
//    }
//
//    default Try<T> recover(Function<Exception, ? extends T> mapper) {
//        return (Try<T>) fold(__ -> this, e -> success(mapper.apply(e)));
//    }
//
//    default Try<T> flatRecover(Function<Exception, Try<T>> mapper) {
//        return fold(__ -> this, e -> mapper.apply(e));
//    }
//
//    default Try<T> flatRecover(Supplier<Try<T>> supl) {
//        return fold(__ -> this, e -> supl.get());
//    }
    
    /**
     * Tries recover failed try with given supplier, has no action for success.
     * @param supplier supplier to recover
     * @param predicate recover attempt happens if predicate returns true.
     * @return this Try for success, new success or new failure depending on if supplier had thrown exception.
     */
    default Try<T> recover(CheckedSupplier<? extends T> supplier, Predicate<Exception> predicate) {
        return fold(__ -> this, e -> predicate.test(e) ? recover(supplier) : this);
    }

    /**
     * Maps Try of type T to type R 
     * @param <R> new result type
     * @param mapper mapper
     * @return new Try of R type or failure if mapper throws exception/
     */
    @SuppressWarnings("unchecked")
    default <R> Try<R> map(CheckedFunction<? super T, ? extends R> mapper) {
        return (Try<R>) flatMap(catching(mapper));
    }

    /**
     * Maps Try of type T to try of type R 
     * @param <R> new result type
     * @param mapper mapper
     * @return new Try of R type or failure if mapper throws exception/
     */
    @SuppressWarnings("unchecked")
    default<R> Try<R> flatMap(Function<? super T, Try<R>> mapper) {
        return fold(mapper::apply, __ -> (Try<R>) this);
    }

    
    /**
     * Filters current Try, has no action for failure.
     * @param predicate predicate to test
     * @return this Try if predicate returns true or new fail with {@link NoSuchElementException}
     */
    default Try<T> filter(Predicate<? super T> predicate) {
        return fold(v -> predicate.test(v) ? this : failure(new NoSuchElementException()), __ -> this);
    }
    
    default Stream<T> stream() {
        return fold(Stream::of, __ -> Stream.empty());
    }
    
    default Optional<T> optional() {
        return fold(Optional::ofNullable, __ -> Optional.empty());
    }
    
    @Override
    void close();

    /**
     * Gets Try's value or throws exception 
     * @return value of T in case of success
     * @throws RuntimeException in case of failure 
     */
    T orElseThrow();
    
    <R> R fold (Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onFailure);

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
    default Try<T> andFinally(CheckedRunnable runnable) {
        try {
            runnable.run();
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
    default Try<T> peek(CheckedConsumer<Try<T>> consumer) {
        try {
            consumer.accept(this);
            return this;
        } catch (Exception e) {
            return failure(e);
        }
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
     * @param exception exception
     * @return Try &lt;T&gt;
     */
    static <T> Try<T> failure(Exception exception) {return new Failure<>(exception);}
    
    /**
     * Factory method to produce Try from supplier that may throw an exception.
     * <p>
     * @param <T> Try's result type
     * @param supplier that gives the result of T type and may throw an exception.
     * @return Try of T type
     * @throws NullPointerException if supplier returns null.
     */
    @SuppressWarnings("unchecked")
    static <T> Try<T> of(CheckedSupplier<? extends T> supplier) {
        return (Try<T>) catching((T t) -> supplier.get()).apply(null);
    }

    /**
     * Factory method to produce Try from runnable that may throw an exception.
     * @param runnable exceptional runnable
     * @return Try of {@link Void} type
     */
    static Try<Void> of(CheckedRunnable runnable) {
        return of(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Higher order function to transform partial {@code T->R} function 
     * to the total {@code T->Try<R>} function.
     * <br>
     * Simplifies using Try with Java's streams and optionals.
     * Sample usage:
     * <pre>{@code 
     *  Stream.of("1", "2")           //Stream<String>
     *  .map(Try.lift(Integer::valueOf) //Stream<Try<Integer>>
     *  .flatMap(Try::stream)         //Stream<Integer>
     *  .toList()                     //List<Integer>
     * }</pre>
     * 
     * @param <T> function parameter type
     * @param <R> function result type
     * @param func partial function {@code T->R}
     * @return total function {@code T->Try<R>}
     */
    static <T, R> Function<T, Try<R>> catching(CheckedFunction<T, R> func) {
        return (T t) -> { 
            try {
                return success(func.apply(t));
            } catch (Exception e) {
                return failure(e);
            }
        };
    }
    
    static <T> Function<T, Try<T>> consumeCatching(CheckedConsumer<T> cons) {
        return (T t) -> { 
            try {
                cons.accept(t);
                return success(t);
            } catch (Exception e) {
                return failure(e);
            }
        };
    }

    static <T> Function<T, Try<T>> getCatching(CheckedSupplier<T> supl) {
        return (T t) -> { 
            try {
                return success(supl.get());
            } catch (Exception e) {
                return failure(e);
            }
        };
    }

    static <T> Function<T, Try<Void>> runCatching(CheckedRunnable run) {
        return (T t) -> { 
            try {
                run.run();
                return success(null);
            } catch (Exception e) {
                return failure(e);
            }
        };
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
        public T orElseThrow() {return value;}
        
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

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onFailure) {
            return onSuccess.apply(value);
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
        
        @Override
        public T orElseThrow() {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }
        
        @Override
        public void close() {/*nothing to close*/}

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onFailure) {
            return onFailure.apply(exception);
        }
    }
    
    static <T> CheckedFunction<T, T> toFunction(CheckedConsumer<? super T> cons) {
        return  (T t) -> {
            cons.accept(t);
            return t;
        };
    }

    /**
     * Converts supplier to function.
     */
    static <U> CheckedFunction<Object, U> toFunction(CheckedSupplier<? extends U> supplier) {
        return (Object o) -> supplier.get();
    }
}
