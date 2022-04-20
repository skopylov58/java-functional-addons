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
//private class EitherLeft<R, L> implements Either<R, L>{
//    protected final L left;
//    
//    EitherLeft(L l) {
//        Objects.requireNonNull(l);
//        left = l;
//    }
//
//    @Override
//    public boolean isLeft() {return true;}
//
//    @Override
//    public R getRight() {
//        throw new NoSuchElementException();
//    }
//
//    @Override
//    public L getLeft() {
//        return left;
//    }
//
////    @Override
////    public Optional<Either<R, L>> filter(Predicate<R> pred) {
////        return Optional.empty();
////    }
//    
//    @Override
//    public Either<R, L> filter(Predicate<R> pred, Supplier<L> supplier) {
//        return this;
//    }
//    
//    @SuppressWarnings("unchecked")
//    @Override
//    public <T> Either<T, L> map(Function<R, T> mapper) {
//        return (Either<T, L>) this;
//    }
//    
//    @Override
//    public Optional<R> optionalRight() {
//        return Optional.empty();
//    }
//    
//    @Override
//    public Optional<R> optional() {
//        return Optional.empty();
//    }
//    
//    @Override
//    public Stream<R> stream() {
//        return Stream.empty();
//    }
//    
//    @Override
//    public R orElse(R def) {
//        return def;
//    }
//    
//    @Override
//    public R orElseGet(Supplier<R> supplier) {
//        Objects.requireNonNull(supplier);
//        return supplier.get();
//    }
//    
//    @Override
//    public Either<R, L> toRight(Supplier<R> supplier) {
//        return Either.right(supplier.get());
//    }
//    
//    @Override
//    public Either<R, L> toRight(Supplier<R> supplier, Predicate<L> predicate) {
//        return predicate.test(left) ? toRight(supplier) : this;
//    }
//    
//}
//
