package com.github.skopylov58.functional.samples;

import com.github.skopylov58.functional.Validator;

public interface SimpleValidator<T> extends Validator<T, String>{

  static <T> SimpleValidator<T> of(T t) {
    return (SimpleValidator<T>) Validator.<T, String>of(t);
  }
  
}
