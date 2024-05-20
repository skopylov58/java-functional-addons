package com.github.skopylov58.functional.monad.pictures;

import java.util.function.Function;

sealed interface Maybe<T> {

  default <R> Maybe<R> map(Function<T, R> mapper) {
    if (this instanceof Just<T> just) {
      return Maybe.just(mapper.apply(just.t));
    } else {
      return Maybe.nothing();
    }
  }

  default <R> Maybe<R> flatMap(Function<T, Maybe<R>> mapper) {
    if (this instanceof Just<T> just) {
      return mapper.apply(just.t);
    } else {
      return Maybe.nothing();
    }
  }
  
  static <T> Maybe<T> just(T t) {
    return new Just<>(t);
  }

  @SuppressWarnings("unchecked")
  static <T> Maybe<T> nothing() {
    return (Maybe<T>) Nothing.INSTANCE;
  }

  record Just<T>(T t) implements Maybe<T> {
  }

  final class Nothing<T> implements Maybe<T> {
    static final Nothing<?> INSTANCE = new Nothing<>();
  }
}
