package com.github.skopylov58.functional;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OptionTest {
	
	@Test
	public void testSomeAndNone() throws Exception {
		
		Option<Integer> someInt = Option.some(1);
		System.out.println(someInt);
		
		Option<Integer> noneInt = Option.none();
		System.out.println(noneInt);
		
		Monad<Integer> lifted = Monad.liftM2(someInt, noneInt, (x, y) -> x + y);
		assertTrue(lifted instanceof Option.None<Integer>);
		System.out.println(lifted);
	}

	@Test
	public void testSomeAndSome() throws Exception {
		
		Option<Integer> someInt1 = Option.some(1);
		System.out.println(someInt1);
		
		Option<Integer> someInt2 = Option.some(2);
		System.out.println(someInt2);
		
		Monad<Integer> lifted = Monad.liftM2(someInt1, someInt2, (x, y) -> x + y);
		assertTrue(lifted instanceof Option.Some<Integer>);
		System.out.println(lifted);
	}


}
