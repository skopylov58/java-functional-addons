package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

public class FilterTest {
    @Test
    public void testFilter() throws Exception {
        Try<Integer> t = Try.success(3);
        Try<Integer> filtered = t.filter(i -> i%2 == 0);
        assertTrue(filtered.isFailure());
        filtered.onFailure(e -> assertEquals(NoSuchElementException.class, e.getClass()));

        t = Try.success(4);
        filtered = t.filter(i -> i%2 == 0);
        assertTrue(filtered.isSuccess());
    }
}
