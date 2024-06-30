package com.github.skopylov58.functional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Simple validator that can collect validation errors.
 * 
 * @param <T> type to validate
 * @param <E> validation error type
 */
public interface Validator<T, E> {
  
  /**
   * Validates validator's object of type T.
   * @param predicate predicate to validate validator's object
   * @param errorSupplier error supplier if predicate returns false.
   * @return this validator
   */
  Validator<T, E> validate(Predicate<T> predicate, Supplier<E> errorSupplier);
  
  /**
   * Validates some internal state (class members, etc.) of validator's object.
   * @param <R>
   * @param mapper
   * @param predicate
   * @param errorSupplier
   * @return
   */
  <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Supplier<E> errorSupplier);
  <R> Validator<T, E> validate(Function<T, R> mapper, Predicate<R> predicate, Function<R, E> errorMapper);
  Validator<T, E> validate(Function<T, Optional<E>> errorChecker);
  
  <R> Validator<T, E> notNull(Function<T, R> mapper, Supplier<E> errorSupplier);
  
  /**
   * Checks if object is valid
   * @return true if no validation errors were found
   */
  boolean hasErrors();
  
  /**
   * Returns validation errors.
   * @return empty list if no errors were found.
   */
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

  /**
   * Factory method to create validator of type T and errors type E
   * @param <T> type to validate
   * @param <E> validation error type
   * @param t object to validate
   * @return validator
   */
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

    @Override
    public Validator<T, E> validate(Function<T, Optional<E>> errorChecker) {
      errorChecker.apply(value).ifPresent(errors::add);
      return this;
    }
  }

}
