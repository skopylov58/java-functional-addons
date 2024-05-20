package com.github.skopylov58.functional.monad.pictures;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

class MonadInPictures {

  interface Functor<T> {
    <R> Functor<R> map(Function<T, R> mapper);
  }

  interface Maybe<T> {
    <R> Maybe<R> map(Function<T, R> mapper);
  }

  record Some<T>(T t) implements Maybe<T> {
    @Override
    public <R> Maybe<R> map(Function<T, R> mapper) {
      return new Some<>(mapper.apply(t));
    }
  }

  record None<T>() implements Maybe<T> {
    @Override
    public <R> Maybe<R> map(Function<T, R> mapper) {
      return new None<>();
    }

  }

  public static void main(String[] args) {

    Maybe<Integer> maybe = new Some<>(6);
    maybe.map(i -> i + 3);
    System.out.println(maybe);
    
  }
  
  <T, R> Optional<R> applicative(Optional<Function<T, R>> func, Optional<T> value) {
    return func.flatMap(f -> value.flatMap(v -> Optional.ofNullable(f.apply(v))));
  }

  <A, B, C> Optional<C> lift_a2(Optional<A> optA, Optional<B> optB, BiFunction<A, B, C> bifunc) {
    return optA.flatMap(a -> optB.flatMap(b -> Optional.of(bifunc.apply(a, b))));
  }
  
  
  void foo() {
    Function<Integer, Integer> mult2 = x -> x * 2;
    Function<Integer, Integer> add3 = x -> x + 3;
    
    var intStream = Stream.of(1,2,3);
    
    Stream.of(mult2, add3).flatMap(f -> Stream.of(1,2,3).map(f)).toList();
  }
  
  
  Optional<Integer> half(Integer value) {
    return value % 2 == 0 ? Optional.of(value/2) : Optional.empty();
  }
  
}


