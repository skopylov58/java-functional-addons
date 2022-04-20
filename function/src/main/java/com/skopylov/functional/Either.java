package com.skopylov.functional;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Minimal functional Either implementation.
 * 
 * @author skopylov@gmail.com
 *
 * @param <R> right side type
 * @param <L> left side type
 */
public interface Either<R, L> {

    boolean isLeft();
    default boolean isRight() {return !isLeft();}
    
    R orElse(R def);
    R orElseGet(Supplier<R> supplier);
    Optional<R> optional();
    Stream<R> stream();
    
    
    R getRight();
    L getLeft();
    
    static <R, L> Either<R, L> right(R right) {
        return new Right<>(right);
    }

    static <R, L> Either<R, L> left(L left) {
        return new Left<>(left);
    }
    
    class Right<R, L> implements Either<R, L> {
        
        protected final R right;
        
        Right (R r ) {right = r;}

        @Override
        public boolean isLeft() {return false;}

        @Override
        public R orElse(R def) {return right;}

        @Override
        public R orElseGet(Supplier<R> supplier) {
            return right;
        }

        @Override
        public Optional<R> optional() {return Optional.ofNullable(right);}

        @Override
        public Stream<R> stream() {return Stream.of(right);}

        @Override
        public R getRight() {
            return right;
        }

        @Override
        public L getLeft() {
            throw new IllegalStateException("This is right");
        }
    }
    
    class Left<R, L> implements Either<R, L> {
        
        protected final L left;
        
        Left(L l) {left = l;}

        @Override
        public boolean isLeft() {return true;}

        @Override
        public R orElse(R def) {
            return def;
        }

        @Override
        public R orElseGet(Supplier<R> supplier) {
            return supplier.get();
        }

        @Override
        public Optional<R> optional() {
            return Optional.empty();
        }

        @Override
        public Stream<R> stream() {
            return Stream.empty();
        }

        @Override
        public R getRight() {
            throw new IllegalStateException("This is left");
        }

        @Override
        public L getLeft() {
            return left;
        }
        
    }
}
    
        

