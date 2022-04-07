package com.skopylov.ftry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.skopylov.ftry.Try;

public class ResourceTest extends TestBase {
    
    @Test
    public void test1() throws Exception {
        CloseableMock closeable = new CloseableMock("res1");
        var r = Try.of(() -> closeable).autoClose().map(c -> 1);
        assertFalse(closeable.isClosed()); // not closed yet
        
        Integer integer = r.get();
        assertTrue(closeable.isClosed()); //closed after get
        System.out.println(integer);
    }

    @Test
    public void test2() throws Exception {
        CloseableMock closeable = new CloseableMock("res2");
        var r = Try.success(closeable).map(c -> 1);
        assertFalse(closeable.isClosed()); // not closed yet
        
        Integer integer = r.get();
        assertFalse(closeable.isClosed()); // not closed after get
        System.out.println(integer);
    }

    @Test
    public void test3() throws Exception {
        CloseableMock closeable = new CloseableMock("res3");
        try {
            Try.success(closeable).map(c -> 1).autoClose();
            fail();
        } catch (AssertionError er) {
            System.out.println(er);
        }
    }
    
    @Test
    public void testTryWithResource() throws Exception {
        CloseableMock closeable = new CloseableMock("res4");
        Try<Integer> tr = Try.success(closeable).autoClose().map(c -> 1);
        try (tr) {} //should close this try
        assertTrue(closeable.isClosed());
    }

}
