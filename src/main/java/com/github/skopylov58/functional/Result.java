//package com.github.skopylov58.functional;
//
//import java.util.function.Function;
//
//public interface Result<T> extends Monad<T> {
//
//
//  static <T> Result<T> success(T t) {
//    return new Success<>(t);
//  }
//
//   static <T> Result<T> failure(Exception e) {
//   return new Failure<>(e);
//   }
//
//  class Success<T> implements Result<T> {
//
//    private final T t;
//
//    private Success(T t) {
//      this.t = t;
//    }
//
//    @Override
//    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
//      return success(mapper.apply(t));
//    }
//
//    @Override
//    public <R> Monad<R> flatMap(Function<? super T, ? extends Monad<R>> mapper) {
//      return mapper.apply(t);
//    }
//
//  }
//  
//  class Failure<T> implements Result<T> {
//
//    private final Exception e;
//    private Failure(Exception e) {
//      this.e = e;
//    }
//    
//    @Override
//    public <R> Monad<R> map(Function<? super T, ? extends R> mapper) {
//      return (Monad<R>) this;
//    }
//
//    @Override
//    public <R> Monad<R> flatMap(Function<? super T, ? extends Monad<R>> mapper) {
//      return (Monad<R>) this;
//    }
//    
//  }
//  
//
//  // <R> Monad<R> flatMap(Function<? super T, ? extends Monad<R>> mapper);
//
//
//  static <T, R> Function<T, Result<R>> lift(FPUtils.CheckedFunction<T, R> mapper) {
//    return t -> {
//      try {
//        R apply = mapper.apply(t);
//        if (apply == null) {
//          return failure(new NullPointerException());
//        }
//        return success(apply);
//      } catch (Exception e) {
//        return failure(e);
//      }
//    };
//  }
//
//}
