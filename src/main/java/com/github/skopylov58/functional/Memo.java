package com.github.skopylov58.functional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simple memoizer.
 * @param <T>
 * @param <R>
 */
public class Memo<T, R> implements Function<T, R> {
	
	private ConcurrentHashMap<T, R> cache = new ConcurrentHashMap<>();
	private Function<T, R> func;
	
	private Memo(Function<T, R> func) {
		this.func = func;
	}
	
	static <T, R> Function<T,R> memoize(Function<T, R> func) {
		return new Memo<>(func);
	}

	@Override
	public R apply(T t) {
		return cache.computeIfAbsent(t, tt -> func.apply(tt));
	}

}
