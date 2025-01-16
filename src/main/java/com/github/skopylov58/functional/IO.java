package com.github.skopylov58.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

interface IO<T> {

  T run();
  
  static <T> IO<T> of(Supplier<T> s) {
    return s::get;
  }

  static <T> IO<T> of(T t) {
    return () -> t;
  }

  static <T> IO<Void> consume(T t, Consumer<T> c) {
    c.accept(t);
    return () -> null;
  }
  
  default <R> IO<R> flatMap(Function<? super T, IO<? extends R>> mapper) {
    return () -> mapper.apply(run()).run();
  }

  default <R> IO<R> map(Function<? super T, ? extends R> mapper) {
    return () -> mapper.apply(run());
  }

}
