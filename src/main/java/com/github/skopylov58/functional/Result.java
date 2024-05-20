package com.github.skopylov58.functional;

import java.util.function.Function;

public interface Result<T> extends Monad<T> {
	
	
//	static <T> Result<T> success(T t) {
//		return new Success<>(t);
//	}

//	static <T> Result<T> failure(Exception e) {
//		return new Success<>(t);
//	}

	
	
	
//	class Success<T> implements Result<T> {
//
//		private final T t;
//		
//		private Success(T t) {
//			this.t = t;
//		}
//		
//		@Override
//		public <R> Result<R> map(Function<T, R> mapper) {
//			return success(mapper.apply(t));
//		}
//
//		@Override
//		public <R> Result<R> flatMap(Function<T, Monad<R>> mapper) {
//			return mapper.apply(t);
//		}
//		
//	}
	
	
	
	

}
