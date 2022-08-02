package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.function.Predicate;

import javax.naming.NoPermissionException;

import org.junit.Test;

public class Try0Test {
    
    @Test
    public void testSupplier() throws Exception {
        
        Try<Integer> tr = Try.of(() -> 20/4);
        assertTrue(tr.isSuccess());
        assertEquals(Integer.valueOf(5), tr.optional().get());
        
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
    public void testSupplierWithNull() {
        Try<Integer> t = Try.of(() -> null);
        assertTrue(t.isSuccess());
        assertTrue(t.optional().isEmpty());
    }    
    
    @Test
    public void testRunnable() throws Exception {
        
        Try<Class<Void>> run = Try.of(() -> System.out.println("Runnable"));
        assertTrue(run.isSuccess());
        assertEquals(Void.TYPE, run.get());

        Try.CheckedRunnable er = () -> {throw new FileNotFoundException();};
        
        run = Try.of(er);
        assertTrue(run.isFailure());
        run.onFailure(e -> assertTrue(e instanceof FileNotFoundException));
        
        try {
            run.get();
            fail();
        } catch (RuntimeException e) {
            //expected
        }
    }
    
    @Test
    public void testFinally() throws Exception {
        Try<Integer> tr = Try.of(() -> 1).andFinally(() -> System.out.println("Finally"));
        assertTrue(tr.isSuccess());
        
        Try.CheckedRunnable er = () -> {throw new FileNotFoundException();};
        tr = Try.of(() -> 1).andFinally(er);
        assertTrue(tr.isFailure());
    }
    
    @Test
    public void testPeek() throws Exception {
        Try.of(() -> 1).peek(t -> assertTrue(t.isSuccess()));
        
        Try<Integer> tr = Try.of(() -> 1).peek(t -> {throw new NoPermissionException();});
        assertTrue(tr.isFailure());
    }
    
    @Test
    public void testError() throws Exception {
        try {
            Try<Integer> t = Try.of(() -> {throw new AssertionError();});
            fail();
        } catch (Error er) {
            // ok we do not handle error exceptions !!!
        }
    }
    
}
