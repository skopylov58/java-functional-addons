package com.github.skopylov58.functional;


import java.util.function.Function;

/**
 * State monad.
 * 
 * @param <T> value type
 * @param <S> state type
 */
@FunctionalInterface
public interface State<T, S> {

  /**
   * State monad is just function S -> (T, S)
   * @param state
   * @return
   */
  Tuple<T, S> apply(S state);

  /**
   * Creates state monad from pure value
   * @param <T> value type
   * @param <S> state type
   * @param t value
   * @return state monad
   */
  static <T, S> State<T, S> pure(T t) {
    return s -> new Tuple<>(t, s);
  }

  default <R> State<R, S> map(Function<? super T, ? extends R> mapper) {
    return state -> {
      Tuple<T, S> tuple = apply(state);
      return new Tuple<>(mapper.apply(tuple.first), tuple.second);
    };
  }

  default <R> State<R, S> flatMap(Function<? super T, State<R, S>> mapper) {
    return state -> {
      Tuple<T, S> tuple = apply(state);
      return mapper.apply(tuple.first).apply(tuple.second);
    };
  }

}
