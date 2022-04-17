package com.skopylov.functional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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

    @Override
    public Optional<Either<R, L>> filter(Predicate<R> pred) {
        return Optional.empty();
    }
    
    @Override
    public Optional<R> optionalRight() {
        return Optional.empty();
    }
    
}

