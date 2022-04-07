package com.skopylov.functional;

import java.util.Objects;

class EitherRight<R, L> implements Either<R, L>{
    final R right;
    
    EitherRight(R r) {
        Objects.requireNonNull(r);
        right = r;
    }

    @Override
    public boolean isLeft() {return false;}

    @Override
    public R getRight() {
        return right;
    }

    @Override
    public L getLeft() {
        throw new IllegalStateException();
    }
    
}

