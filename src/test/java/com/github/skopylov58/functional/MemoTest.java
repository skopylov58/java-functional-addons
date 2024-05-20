package com.github.skopylov58.functional;

import java.time.Duration;
import java.util.function.Function;

import org.junit.Test;

public class MemoTest {
	
	
	@Test
	public void test() throws Exception {
		
		Function<Integer, Integer> add3 = x -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return x + 3;
		};
		
		
		Function<Integer, Integer> memoized = Memo.memoize(add3);
		
		Duration duration = TryUtils.measure(() -> {
			memoized.apply(2);
		});
		System.out.println("First call: " + duration);

		duration = TryUtils.measure(() -> {
			memoized.apply(2);
		});
		System.out.println("Second call: " + duration);
	}

}
