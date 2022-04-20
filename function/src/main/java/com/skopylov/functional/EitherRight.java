//package com.skopylov.functional;
//
//import java.util.NoSuchElementException;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.function.Supplier;
//import java.util.stream.Stream;
//
//private class EitherRight<R, L> implements Either<R, L>{
//    protected final R right;
//    
//    EitherRight(R r) {
//        Objects.requireNonNull(r);
//        right = r;
//    }
//
//    @Override
//    public boolean isLeft() {return false;}
//
//    @Override
//    public R getRight() {
//        return right;
//    }
//
//    @Override
//    public L getLeft() {
//        throw new NoSuchElementException();
//    }
//    
////    @Override
////    public Optional<Either<R, L>> filter(Predicate<R> pred) {
////        return pred.test(right) ? Optional.of(this) : Optional.empty();
////    }
//    
//    @Override
//    public Either<R, L> filter(Predicate<R> pred, Supplier<L> supplier) {
//        return pred.test(right) ? this : Either.left(supplier.get());
//    }
//    
//    @Override
//    public <T> Either<T, L> map(Function<R, T> mapper) {
//        return Either.right(mapper.apply(right));
//    }
//    
//    @Override
//    public Optional<R> optionalRight() {
//        return Optional.ofNullable(right);
//    }
//    
//    @Override
//    public Optional<R> optional() {
//        return Optional.ofNullable(right);
//    }
//    
//    @Override
//    public Stream<R> stream() {
//        return Stream.of(right);
//    }
//    
//    @Override
//    public R orElse(R def) {
//        return right;
//    }
//    
//    @Override
//    public R orElseGet(Supplier<R> supplier) {
//        return right;
//    }
//    
//    @Override
//    public Either<R, L> toRight(Supplier<R> supplier) {
//        return this;
//    }
//    
//    @Override
//    public Either<R, L> toRight(Supplier<R> supplier, Predicate<L> predicate) {
//        return this;
//    }
//}
//
