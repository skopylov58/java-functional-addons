package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Supplier;

import org.junit.Test;

public class SupplierTest {
    @Test
    public void testSupplier() throws Exception {
        
        Try<Integer> tr = Try.of(() -> 20/4);
        assertTrue(tr.isSuccess());
        assertEquals(Integer.valueOf(5), tr.get());
        
        tr = Try.of(() -> {throw new NullPointerException();});
        assertTrue(tr.isFailure());
        
        tr.onFailure(e -> assertTrue(e instanceof NullPointerException));
        try {
            tr.get();
            fail();
        } catch (NullPointerException npe) {
            //expected
        }
    }

    @Test
    public void testPureSupplier() {
        Supplier<Integer> s = () -> 1;
        // Try.of(s); - will not compile
        Try<Integer> t = Try.of(() -> s.get());
        assertEquals(true, t.isSuccess());
        assertEquals(Integer.valueOf(1), t.get());
    }
    
    @Test
    public void testSupplierWithNull() {
        Try<Integer> t = Try.of(() -> null);
        assertTrue(t.isSuccess());
        assertTrue(t.optional().isEmpty());
    }    
    
}
