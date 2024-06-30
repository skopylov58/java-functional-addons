package com.github.skopylov58.functional;

import java.util.function.Function;

/**
 * Reader monad.
 * 
 * @param C context type
 * @param T value type
 */
@FunctionalInterface
interface Reader<C, T> {

  /**
   * Reader monad is just function C -> T where C is context, T is value
   * 
   * @param context
   * @return value of type T
   */
  T run(C context);

  /**
   * Creates Reader monad from value of type T
   * @param <C> context type
   * @param <T> value type
   * @param t value
   * @return Reader<C, T> monad
   */
  static <C, T> Reader<C, T> pure(T t) {
    return ctx -> t;
  }

  default <R> Reader<C, R> map(Function<? super T, ? extends R> mapper) {
    return ctx -> {
      return mapper.apply(run(ctx));
    };
  }

  default <R> Reader<C, R> flatMap(Function<? super T, Reader<C, R>> mapper) {
    return ctx -> {
      T t = run(ctx);
      Reader<C, R> r = mapper.apply(t);
      return r.run(ctx);
    };
  }

}
