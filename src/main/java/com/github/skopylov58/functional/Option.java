package com.github.skopylov58.functional;

import java.util.function.Function;

public interface Option<T> extends Monad <T> {
	
	static <T> Option<T> of(T t) {
		return t == null ? none() : some(t);
	}
	
	static <T> Option<T> some(T t) {
		return new Some<>(t);
	}

	static <T> Option<T> none() {
		return new None<>();
	}
	
	class Some<T> implements Option<T> {

		final T t;
		private Some(T t) {
			this.t = t;
		}
		
		@Override
		public <R> Option<R> map(Function<? super T, ? extends R> mapper) {
			return some(mapper.apply(t));
		}
 
		@Override
		public <R> Option<R> flatMap(Function<? super T, ? extends Monad<R>> mapper) {
			Monad<R> monad = mapper.apply(t);
			if (monad instanceof Option<R>) {
				return (Option<R>) monad;
			} else if (monad instanceof Result<R>) {
				return null;
			}
			
			
			return (Option<R>) mapper.apply(t);
		}
		
		@Override
		public String toString() {
			return "Some: " + t.toString();
		}
	}

	class None<T> implements Option<T> {

		private None() {}
		
		@Override
		public <R> Monad<R> map(Function<? super T, ? extends R> mapper) {
			return (Monad<R>) this;
		}

		@Override
		public <R> Monad<R> flatMap(Function<? super T, ? extends Monad<R>> mapper) {
			return (Monad<R>) this;
		}
		
		@Override
		public String toString() {
			return "None";
		}
		
		
		void test() {
			
			Function<Integer, Number> f = x -> 1.0d;
			
		}
	}
}
