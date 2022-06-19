package com.skopylov.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Test;

public class SimpleTryTest {
    
    @Test
    public void test0() throws Exception {
        
        SimpleTry<Integer> etry = SimpleTry.of(() -> 1);
        assertTrue(etry.optional().isPresent());
    }

    @Test
    public void testResource() throws Exception {
        
        String fileName = "/ab/c/";
        var reader = SimpleTry.of(() -> new BufferedReader(new FileReader(fileName)))
            .closeable()
            //.map(r -> r.lines())
            .map(BufferedReader::lines)
            .optional()
            .stream();
            
    }
    
    @Test
    public void testNull() throws Exception {
        var opt = SimpleTry.of(() -> (Integer) null)
        .peek(t -> assertTrue(t.isSuccess()))
        .filter(Objects::nonNull)
        .peek(t -> assertTrue(t.isFailure()))
        .onFailure(e -> assertTrue(e instanceof NoSuchElementException))
        .optional();
        assertTrue(opt.isEmpty());
    }
    
    @Test
    public void testOptional() throws Exception {
        var result = SimpleTry.of(() -> Optional.of(Integer.valueOf(1)))
        .optional()
        .flatMap(Function.identity())
        .get();
        assertEquals(1, result.intValue());
    }
    
    
    @Test
    public void testStream() throws Exception {
        var result = SimpleTry.of(() -> Stream.of(1))
        .stream()
        .flatMap(Function.identity())
        .findFirst()
        .get();
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testRunnable() throws Exception {
        SimpleTry.of(() -> System.out.println("foo"));
    }
    
}
