package com.skopylov.functional;

import java.util.Optional;
import java.util.function.Predicate;

public interface Either<R, L> {

    boolean isLeft();
    default boolean isRight() {return !isLeft();}
    
    Optional<Either<R, L>> filter(Predicate<R> pred);
    
    Optional<R> optionalRight();
    
    R getRight();
    L getLeft();
    
    static <R, L> Either<R, L> right(R right) {
        return new EitherRight<>(right);
    }

    static <R, L> Either<R, L> left(L left) {
        return new EitherLeft<>(left);
    }
    
        

}
