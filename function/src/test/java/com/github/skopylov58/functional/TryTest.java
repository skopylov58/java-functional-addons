package com.github.skopylov58.functional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import javax.naming.NoPermissionException;

import org.junit.Test;

public class TryTest {
    
    
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
