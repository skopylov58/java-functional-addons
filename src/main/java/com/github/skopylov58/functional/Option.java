package com.github.skopylov58.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Option<T> extends Monad<T> {

  static <T> Option<T> of(T t) {
    return t == null ? none() : some(t);
  }

  static <T> Option<T> some(T t) {
    return new Some<>(t);
  }

  static <T> Option<T> none() {
    return NONE;
  }

  static None NONE = new None<>();
  
  record Some<T>(T t) implements Option<T> {

    @Override
    public <R> Option<R> map(Function<? super T, ? extends R> mapper) {
      return some(mapper.apply(t));
    }

    public <R> Option<R> flatMap(Function<? super T, ? extends Option<R>> mapper) {
      return mapper.apply(t);
    }

    @Override
    public String toString() {
      return "Some: " + t.toString();
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

  record None<T>() implements Option<T> {

    @Override
    public <R> Option<R> map(Function<? super T, ? extends R> mapper) {
      return (Option<R>) this;
    }

    public <R> Option<R> flatMap(Function<? super T, ? extends Option<R>> mapper) {
      return (Option<R>) this;
    }

    @Override
    public String toString() {
      return "None";
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
}
