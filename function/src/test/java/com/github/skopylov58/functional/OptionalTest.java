package com.github.skopylov58.functional;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.processing.SupportedSourceVersion;

import org.junit.Test;

public class OptionalTest {

    @Test
    public void test() throws Exception {
        
        Optional<Integer> opt = Try.of(() -> 1).optional();
        assertTrue(opt.isPresent());
        assertEquals(1, opt.get().intValue());

        Try<Integer> t = Try.of(() -> {throw new NullPointerException();});
        assertTrue(t.isFailure());
        opt = t.optional();
        assertFalse(opt.isPresent());
        assertEquals(Optional.empty(), opt);
    }

    @Test
    public void testTryOfOptional() throws Exception {
        
        Try<Optional<Integer>> tried = Try.of(() -> Optional.of(1));
        
        Optional<Integer> opt = tried
                .optional()  //Optional<Optional<Integer>>
                .flatMap(Function.identity());
        
        assertTrue(opt.isPresent());
        assertEquals(1, opt.get().intValue());
        
    }
    
    @Test
    public void testMoney() throws Exception {
        double three = 3.0;
        double one = 1.0;
        
        double third = one /three;
        
        System.out.println(Double.compare(one, three * third));
        
        
        System.out.println(third);
    }

    @Test
    public void sumDoubles() throws Exception {
        double third = 1.0d/3.0d;
        double sum = 0;
        
        int loops = 3_000_000;
        double expected = loops/3;
        
        for (int i = 0; i < loops; i++) {
            sum += third;
        }
        System.out.println("expected: " + expected);
        System.out.println("res: " + sum);
        System.out.println("compare: " + Double.compare(expected, sum));
        System.out.println("diff: " + (expected - sum));
    }

    @Test
    public void multiplyDoubles() throws Exception {
        double third = 1.0d/3.0d;
        double sum = 0;
        
        int loops = 3_000_000;
        double expected = loops/3;

        sum = third * loops;
//        for (int i = 0; i < loops; i++) {
//            sum += third;
//        }
        System.out.println("expected: " + expected);
        System.out.println("res:      " + sum);
        System.out.println("compare: " + Double.compare(expected, sum));
        System.out.println("diff: " + (expected - sum));
    }

    
    
    @Test
    public void bar() throws Exception {
        
        double third = 1.0d/3.0d;
        System.out.println("Double.toString: " + Double.toString(third));
        BigDecimal tenth = BigDecimal.valueOf(third);
        BigDecimal res = BigDecimal.ZERO;
        
        for (int i = 0; i< 3; i++) {
            res = res.add(tenth);
        }
        
        System.out.println(res);
    }
    
}
