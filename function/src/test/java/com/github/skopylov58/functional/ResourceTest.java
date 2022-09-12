package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class ResourceTest {
    
    @Test
    public void testTryWithResource() throws Exception {
        CloseableMock closeable = new CloseableMock("res4");
        try (var tr = Try.success(closeable)) {
            tr.map(c -> 1);
        } //should close this try
        assertTrue(closeable.isClosed());
    }

    @Test
    public void testNotCloseable() throws Exception {
        try (var tr = Try.success(1)) {
            tr.map(c -> 2);
        } catch (IllegalStateException e) {
            //expected
        }
    }
    
    @Test
    public void testFailedSocket () {
        try (var tr = Try.of(() -> new Socket("foo", 2234))) {
            
            tr.map(c -> 2);
            
            assertEquals(false, tr.isSuccess());
            tr.onFailure(e -> assertEquals(UnknownHostException.class, e.getClass()));
        }
    }
    
    class CloseableMock implements AutoCloseable {
        private final String name;
        boolean closed = false;
        
        CloseableMock(String name) {
            this.name = name;
        }
        @Override
        public void close() throws Exception {
            closed = true;
        }
        @Override
        public String toString() {
            return name + " closed: " + closed;
        }
        boolean isClosed() {return closed;}
    }

}
