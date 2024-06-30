package com.github.skopylov58.functional;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.Test;

public class ChainOfResponsibilityTest {



  @SafeVarargs
  public static <T> Consumer<T> chain(Consumer<T> head, Consumer<T>... tail) {
    return Arrays.stream(tail).reduce(head, (a, b) -> a.andThen(b));
  };

  @SafeVarargs
  public static <T> Function<T, T> chain(Function<T, T> head, Function<T, T>... tail) {
    return Arrays.stream(tail).reduce(head, (a, b) -> a.andThen(b));
  };

  @Test
  public void testName() throws Exception {

    Consumer<Integer> chain = chain(
        (Integer i) -> System.out.println(i),
        (Integer i) -> System.out.println(i + 1), 
        (Integer i) -> System.out.println(i + 2)
        );

    chain.accept(1);
  }
}
