package com.skopylov.functional;

import java.util.Objects;

class EitherLeft<R, L> implements Either<R, L>{
    final L left;
    
    EitherLeft(L l) {
        Objects.requireNonNull(l);
        left = l;
    }

    @Override
    public boolean isLeft() {return true;}

    @Override
    public R getRight() {
        throw new IllegalStateException();
    }

    @Override
    public L getLeft() {
        return left;
    }
    
}

