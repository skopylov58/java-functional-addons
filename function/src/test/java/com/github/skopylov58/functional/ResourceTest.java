package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.skopylov58.functional.Try;

public class ResourceTest extends TestBase {
    
    @Test
    public void test() throws Exception {
        CloseableMock closeable = new CloseableMock("res1");
        var res = Try.of(() -> closeable);
        try (res) {
            Try<Integer> i = res.map(c -> 1);
            assertFalse(closeable.isClosed()); // not closed yet
            Integer integer = i.orElseThrow();
            assertEquals(Integer.valueOf(1), integer);
            System.out.println(integer);
        }
        assertTrue(closeable.isClosed()); //closed at this moment
    }

    @Test
    public void testTryWithResource() throws Exception {
        CloseableMock closeable = new CloseableMock("res4");
        try (var tr = Try.success(closeable)) {
            tr.map(c -> 1);
        } //should close this try
        assertTrue(closeable.isClosed());
    }

}
