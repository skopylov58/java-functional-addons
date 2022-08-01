package com.github.skopylov58.functional;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.skopylov58.functional.Try;

public class TryTest {

    @Test
    public void test0() throws Exception {
       
        Try<Integer> t = Try.success(3);
        
        Try<Integer> filtered = t.filter(i -> i%2 == 0);
        assertTrue(filtered.isFailure());
        
    }

    @Test
    public void test1() throws Exception {
        Try<Integer> t = Try.success(3);
    }
    
    @Test
    public void testRunnableLambda() throws Exception {
        Try<Class<Void>> of = Try.of(() -> System.out.println("Runnable"));
        assertTrue(of.isSuccess());
    }

    @Test
    public void testPureRunnable() throws Exception {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Run runnable");
            }
        };
        assertTrue(Try.of(() -> r.run()).isSuccess());
    }

    @Test
    public void testNull() throws Exception {
        var opt = Try.of(() -> null)
        .peek(t -> assertTrue(t.isSuccess()))
        .optional();
        assertTrue(opt.isEmpty());
    }

}
