package com.github.skopylov58.functional;

import java.util.function.Function;

@FunctionalInterface
public interface Functor<T> {
  <R> Functor<R> map(Function<T, R> mapper);
}
