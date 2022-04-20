package com.skopylov.functional;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.Test;

import com.skopylov.functional.Try;

public class TryTest {

    @Test
    public void test0() throws Exception {
       
        Try<Integer> t = Try.success(3);
        
        Try<Integer> filtered = t.filter(i -> i%2 == 0);
        assertTrue(filtered.isLeft());
        
    }

    @Test
    public void test1() throws Exception {
       
        Try<Integer> t = Try.success(3);
        Try<Integer> filtered = t.filter(i -> {throw new FileNotFoundException();});
        
//        Either<Integer, Exception> e = t.filter(i -> i%2 == 0, Exception::new);
//        assertTrue(e.isLeft());
        
    }
    
    @Test
    public void testRunnable() throws Exception {
        
        Try<Class<Void>> of = Try.of(() -> System.out.println("Runnable"));
        assertTrue(of.isSuccess());
    }

    
    
    
}
