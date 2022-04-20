package com.skopylov.functional;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import javax.naming.NoPermissionException;

import org.junit.Test;

import com.skopylov.functional.ExceptionalPredicate;
import com.skopylov.functional.ExceptionalRunnable;
import com.skopylov.functional.TryException;

public class Try0Test {
    
    @Test
    public void testSupplier() throws Exception {
        
        Try<Integer> tr = Try.of(() -> 20/4);
        assertTrue(tr.isSuccess());
        assertEquals(Integer.valueOf(5), tr.orElseThrow());
        
        tr = Try.of(() -> {throw new NullPointerException();});
        assertTrue(tr.isFailure());
        Exception exception = tr.getFailureCause().get();
        assertNotNull(exception);
        assertEquals(NullPointerException.class, exception.getClass());
        try {
            tr.orElseThrow();
            fail();
        } catch (NullPointerException npe) {
            //expected
        }
        
    }
    
//    @Test
//    public void testCompletableFuture() throws Exception {
//        
//        CompletableFuture<Integer> f = new CompletableFuture<>();
//        f.complete(5);
//        Try<Integer> tr = Try.of(f);
//        assertTrue(tr.isSuccess());
//        
//        f = new CompletableFuture<>();
//        f.completeExceptionally(new FileNotFoundException());
//        tr = Try.of(f);
//        assertTrue(tr.isFailure());
//        Exception cause = tr.getFailureCause().get();
//        assertEquals(ExecutionException.class, cause.getClass());
//        cause = (Exception) cause.getCause();
//        assertEquals(FileNotFoundException.class, cause.getClass());
//    }
    
    @Test
    public void testRunnable() throws Exception {
        
        Try<Class<Void>> run = Try.of(() -> System.out.println("Runnable"));
        assertTrue(run.isSuccess());
        assertEquals(Void.TYPE, run.orElseThrow());

        ExceptionalRunnable er = () -> {throw new FileNotFoundException();};
        
        run = Try.of(er);
        assertTrue(run.isFailure());
        Optional<Exception> opt = run.getFailureCause();
        assertTrue(opt.isPresent());
        Exception exception = opt.get();
        assertEquals(FileNotFoundException.class, exception.getClass());
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
        
        ExceptionalRunnable er = () -> {throw new FileNotFoundException();};
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
    
    @Test
    public void testPredicate() throws Exception {
        
        Predicate<Integer> p = i -> false;
        ExceptionalPredicate<Integer> exp = i -> false;
        
        //ExceptionalPredicate<T> cheched(Predicate<T> pred) {return t -> pred.test();};
        
//        assertTrue(Try.success(1).filter(p).isFailure());
//        assertTrue(Try.success(1).filter(exp).isFailure());
    }
    
}
