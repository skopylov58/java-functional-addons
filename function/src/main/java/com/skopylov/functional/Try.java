package com.skopylov.functional;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public interface Try<T> extends Either<T, Exception>, AutoCloseable {

    ThreadLocal<List<AutoCloseable>> resources = ThreadLocal.withInitial(ArrayList::new);  

    static <T> Try<T> success(T value) {return new Success<>(value);}

    static <T> Try<T> failure(Exception value) {return new Failure<>(value);}
    
    static <T> Try<T> of(ExceptionalSupplier<T> supplier) {
        try {
            return success(supplier.getWithException());
        } catch (Exception e) {
            return failure(e);
        }
    }

    static <T> Try<Optional<T>> ofNullable(ExceptionalSupplier<T> supplier) {
        return of(() -> Optional.ofNullable(supplier.getWithException()));
    }

    static Try<Class<Void>> of(ExceptionalRunnable runnable) {
        return of(() -> {
            runnable.runWithException();
            return Void.TYPE;
        });
    }
    
    class Success<T> extends EitherRight<T, Exception> implements Try<T> {
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
    
    class Failure<T> extends EitherLeft<T, Exception> implements Try<T> {
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

    boolean isSuccess();
    
    default boolean isFailure() {return !isSuccess();}
    
    @SuppressWarnings("unchecked")
    default <R> Try<R> cast(Class<R> c) {
        return (Try<R>) this;
    }
    
    default void close() {
        closeResources();
    }
    
    default T get() {
        return orElseThrow();
    }
    
    default Try<T> andFinally(ExceptionalRunnable runnable) {
        try {
            runnable.runWithException();
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    default Try<T> peek(ExceptionalConsumer<Try<T>> consumer) {
        try {
            consumer.acceptWithException(this);
            return this;
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    
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

    
    T orElseThrow();

    Try<T> autoClose();
    
//    @Override
//    T orElse(T defaultValue);
    
    Optional<Exception> getFailureCause();
    
    <X extends Throwable> T orElseThrow(Supplier<? extends X> s) throws X;
    
    Try<T> onSuccess(ExceptionalConsumer<T> consumer);

    Try<T> onFailure(ExceptionalConsumer<Exception> consumer);

    Try<T> recover(ExceptionalSupplier<T> supplier);

    Try<T> recover(ExceptionalSupplier<T> supplier, ExceptionalPredicate<Exception> predicate);

    <R> Try<R> map(ExceptionalFunction<T, R> mapper); 
    
    Try<T> filter(ExceptionalPredicate<T> pred);
    Try<T> filter(ExceptionalPredicate<T> pred, ExceptionalConsumer<T> consumer);
    
}
