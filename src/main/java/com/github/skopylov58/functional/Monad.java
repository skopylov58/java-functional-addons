package com.github.skopylov58.functional;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Monad<T> {
	
	<R> Monad<R> map(Function<? super T, ? extends R> mapper);
	
	<R> Monad<R> flatMap(Function<? super T, ? extends Monad<R>> mapper);
	
	static <A, B, C> Monad<C> liftM2(Monad<A> am, Monad<B> bm, BiFunction<A, B, C> mapper) {
		return am.flatMap(a -> bm.map(b -> mapper.apply(a, b)));
	}

}

//Function<? super T,? extends R> mapper