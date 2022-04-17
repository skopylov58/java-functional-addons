package com.skopylov.functional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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
    
    @Override
    public Optional<Either<R, L>> filter(Predicate<R> pred) {
        return pred.test(right) ? Optional.of(this) : Optional.empty();
    }
    
    @Override
    public Optional<R> optionalRight() {
        return Optional.ofNullable(right);
    }
}

