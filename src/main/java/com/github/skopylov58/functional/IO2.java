package com.github.skopylov58.functional;

import java.util.function.Function;

public interface IO2<C, T>  {
  
  Monad<T> run(C context);

  default Monad<T>run() {
    return run(null);
  }

  default <R> IO2<C, R> map(Function<? super T, ? extends R> mapper) {
    return ctx -> {
      Monad<T> monad = run(ctx);
      return monad.map(mapper);
    };
  }
  
  default <R> IO2<C, R> flatMap(Function<? super T, IO2<C, R>> mapper) {
    return ctx -> {
      Monad<T> monad = run(ctx);
      return monad.bind(t -> mapper.apply(t).run(ctx));
    };
  }

  static <T> IO2<Void, T> of(T t) {
    return c -> Option.some(t);
  }

}
