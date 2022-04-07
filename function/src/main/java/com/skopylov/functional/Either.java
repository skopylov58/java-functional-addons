package com.skopylov.functional;

public interface Either<R, L> {

    boolean isLeft();
    default boolean isRight() {return !isLeft();}
    
    R getRight();
    L getLeft();
    
    static <R, L> Either<R, L> right(R right) {
        return new EitherRight<>(right);
    }

    static <R, L> Either<R, L> left(L left) {
        return new EitherLeft<>(left);
    }

}
