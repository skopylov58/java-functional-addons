package com.github.skopylov58.functional;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Validator<T, E> {
  
  Validator<T, E> validate(Predicate<T> predicate, Supplier<E> errorSupplier);
  <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Supplier<E> errorSupplier);
  <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Function<R, E> errorMapper);
  
  <R> Validator<T, E> notNull(Function<T, R> mapper, Supplier<E> errorSupplier);
  
  boolean hasErrors();
  List<E> getErrors();
  
  default Validator<T, E> validate(Predicate<T> predicate, E error) {
    return validate(predicate, () -> error);
  }

  default <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, E error) {
    return validate(mapper, predicate, () -> error);
  }

  default <R> Validator<T, E> notNull(Function<T, R> member, E error) {
    return notNull(member, () -> error);
  }

  static <T, E>  Validator<T, E> of(T t) {
    return new ValidatorImpl<>(t);
  }

  class ValidatorImpl<T, E> implements Validator<T, E> {
    private final T value;
    private final List<E> errors = new LinkedList<>();
    
    private ValidatorImpl(T t) {
      value = t;
    }

    public Validator<T, E> validate(Predicate<T> predicate, Supplier<E> errorSupplier) {
      return validate(Function.identity(), predicate, errorSupplier);
    }

    @Override
    public <R> Validator<T, E> notNull(Function<T, R> mapper, Supplier<E> errorSupplier) {
      if (mapper.apply(value) == null) {
        errors.add(errorSupplier.get());
      }
      return this;
    }

    @Override
    public <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Supplier<E> errorSupplier) {
      return validate(mapper, predicate, x -> errorSupplier.get());
    }
    
    @Override
    public <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Function<R, E> errorMapper) {
      R apply = mapper.apply(value);
      if (apply == null || !predicate.test(apply)) {
        errors.add(errorMapper.apply(apply));
      }
      return this;
    }

    @Override
    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    @Override
    public List<E> getErrors() {
      return errors;
    }

  }

}
