package com.skopylov.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Either<R, L> {

    boolean isLeft();
    default boolean isRight() {return !isLeft();}
    
    //Optional<Either<R, L>> filter(Predicate<R> pred);
    
    Either<R, L> filter(Predicate<R> pred, Supplier<L> supplier);

    <T> Either<T, L> map(Function<R, T> mapper);
    
    R orElse(R def);
    R orElseGet(Supplier<R> supplier);
    
    Either<R, L> toRight(Supplier<R> supplier);
    Either<R, L> toRight(Supplier<R> supplier, Predicate<L> predicate);
    
    
    
    Optional<R> optionalRight();
    
    Optional<R> optional();
    Stream<R> stream();
    
    
    R getRight();
    L getLeft();
    
    static <R, L> Either<R, L> right(R right) {
        return new EitherRight<>(right);
    }

    static <R, L> Either<R, L> left(L left) {
        return new EitherLeft<>(left);
    }
    
        

}
