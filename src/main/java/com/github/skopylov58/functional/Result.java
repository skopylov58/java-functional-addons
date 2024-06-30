package com.github.skopylov58.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Result monad
 * @param <T> value type
 */
public interface Result<T> extends Monad<T> {

  static <T> Result<T> success(T t) {
    return new Success<>(t);
  }

   static <T> Result<T> failure(Exception e) {
   return new Failure<>(e);
   }

  class Success<T> implements Result<T> {

    private final T t;

    private Success(T t) {
      this.t = t;
    }

    @Override
    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
      return success(mapper.apply(t));
    }

    public <R> Result<R> flatMap(Function<? super T, ? extends Result<R>> mapper) {
      return mapper.apply(t);
    }

    @Override
    public <R> Monad<R> bind(Function<? super T, ? extends Monad<R>> mapper) {
      return mapper.apply(t);
    }

    @Override
    public T getOrDefault(T defaultValue) {
      return t;
    }

    @Override
    public Stream<T> stream() {
      return Stream.of(t);
    }

    @Override
    public Optional<T> optional() {
      return Optional.ofNullable(t);
    }

  }
  
  class Failure<T> implements Result<T> {

    private final Exception e;
    private Failure(Exception e) {
      this.e = e;
    }
    
    @Override
    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
      return (Result<R>) this;
    }

    public <R> Result<R> flatMap(Function<? super T, ? extends Result<R>> mapper) {
      return (Result<R>) this;
    }

    @Override
    public <R> Monad<R> bind(Function<? super T, ? extends Monad<R>> mapper) {
      return (Monad<R>) this;
    }

    @Override
    public T getOrDefault(T defaultValue) {
      return defaultValue;
    }

    @Override
    public Stream<T> stream() {
      return Stream.empty();
    }

    @Override
    public Optional<T> optional() {
      return Optional.empty();
    }
    
  }
  
  static <T, R> Function<T, Result<R>> lift(FPUtils.CheckedFunction<T, R> mapper) {
    return t -> {
      try {
        R apply = mapper.apply(t);
        if (apply == null) {
          return failure(new NullPointerException());
        }
        return success(apply);
      } catch (Exception e) {
        return failure(e);
      }
    };
  }

}
