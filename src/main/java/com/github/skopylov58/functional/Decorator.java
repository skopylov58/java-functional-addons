package com.github.skopylov58.functional;

import java.util.function.Consumer;
import java.util.function.Function;

public class Decorator<T, R> implements Function<T, R> {
	
	private final Consumer<T> before;
	private final Function<T, R> func;
	
	private Decorator(Function<T, R> func, Consumer<T> consumer) {
		this.func = func;
		before = consumer;
	}
	
	public static <T, R> Function<T, R> decorate(Function<T, R> func, Consumer<T> before ) {
		return new Decorator<>(func, before);
	}

	@Override
	public R apply(T t) {
		before.accept(t);
		return func.apply(t);
	}

}
