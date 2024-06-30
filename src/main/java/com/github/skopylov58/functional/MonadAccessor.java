package com.github.skopylov58.functional;

import java.util.Optional;
import java.util.stream.Stream;

public interface MonadAccessor<T> {
  
  T getOrDefault(T defaultValue);
  
  Stream<T> stream();
  
  Optional<T> optional();

}
