package com.skopylov.functional;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface SimpleTry<T> extends AutoCloseable{
    
    @FunctionalInterface
    interface CheckedFunction<T, R> {R apply(T t) throws Exception;}
    @FunctionalInterface
    interface CheckedSupplier<T> {T get() throws Exception;}
    @FunctionalInterface
    interface CheckedRunnable {void run() throws Exception;}

    static <T> SimpleTry<T> of(CheckedSupplier<T> supplier) {
        try {
            return new TryImpl<>(supplier.get());
        } catch (Exception e) {
            return new TryImpl<>(e);
        }
    }

    static SimpleTry<Class<Void>> of(CheckedRunnable runnable) {
        return of(() -> {runnable.run(); return Void.TYPE;});
    }

    boolean isFailure();
    default boolean isSuccess() {return !isFailure();}
    Stream<T> stream();
    Optional<T> optional();
    <R> SimpleTry<R> map(CheckedFunction<T, R> mapper);
    SimpleTry<T> filter(Predicate<T> predicate);
    SimpleTry<T> onFailure(Consumer<Exception> consumer);
    SimpleTry<T> closeable();
    SimpleTry<T> recover(CheckedSupplier<T> supplier);
    
    default SimpleTry<T> andFinally(Runnable runnable) {
        runnable.run();
        return this;
    }
    default SimpleTry<T> peek(Consumer<SimpleTry<T>> consumer) {
        consumer.accept(this);
        return this;
    }
    
    class TryImpl<T> implements SimpleTry<T>{
        private Either<T, Exception> either;
        private List<AutoCloseable> resources;

        private TryImpl(T t) {either = Either.right(t);}
        private TryImpl(T t, List<AutoCloseable> resources) {
            this(t);
            this.resources = resources;
        }
        private TryImpl(Exception e) {either = Either.left(e);}
        
        @Override
        public Stream<T> stream() {
            return either.isRight() ? Stream.of(either.getRight()) : Stream.empty();
        }

        @Override
        public Optional<T> optional() {
            return either.isRight() ? Optional.ofNullable(either.getRight()) : Optional.empty();
        }

        @Override
        public SimpleTry<T> filter(Predicate<T> predicate) {
            if (either.isRight()) {
                if (!predicate.test(either.getRight())) {
                    either = Either.left(new NoSuchElementException());
                }
            }
            return this;
        }

        @Override
        public <R> SimpleTry<R> map(CheckedFunction<T, R> mapper) {
            if (either.isRight()) {
                try {
                    return new TryImpl<>(mapper.apply(either.getRight()), resources);
                } catch (Exception e) {
                    either = Either.left(e);
                }
            }
            return (SimpleTry<R>) this;
        }

        @Override
        public SimpleTry<T> onFailure(Consumer<Exception> consumer) {
            if (either.isLeft()) {
                consumer.accept(either.getLeft());
            }
            return this;
        }

        @Override
        public void close() {
            if (resources != null) {
                resources.forEach(r -> SimpleTry.of(r::close));
                resources.clear();
                resources = null;
            }
        }

        @Override
        public SimpleTry<T> closeable() {
            if (either.isRight()) {
                if (either.getRight() instanceof AutoCloseable) {
                    if (resources == null) {
                        resources = new LinkedList<>();
                    }
                    resources.add((AutoCloseable) either.getRight());
                } else {
                    throw new IllegalArgumentException(either.getRight() + " should be AutoCloseable");
                }
            }
            return this;
        }

        @Override
        public boolean isFailure() { return either.isLeft();}
        
        @Override
        public SimpleTry<T> recover(CheckedSupplier<T> supplier) {
            if (either.isLeft()) {
                try {
                    either = Either.right(supplier.get());
                } catch (Exception e) {
                    either = Either.left(e);
                }
            }
            return this;
        }
    }
}

