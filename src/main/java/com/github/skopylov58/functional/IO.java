package com.github.skopylov58.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IO<T> {

  T run();
  
  public static <T> IO<T> of(Supplier<T> s) {
    return s::get;
  }

  public static <T> IO<T> of(T t) {
    return () -> t;
  }

  public static <T> IO<Void> consume(T t, Consumer<T> c) {
    c.accept(t);
    return () -> null;
  }
  
  default <R> IO<R> flatMap(Function<T, IO<R>> mapper) {
    return () -> mapper.apply(run()).run();
  }
}
