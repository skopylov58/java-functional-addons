package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.Test;

public class RunnableTest {
    
    @Test
    public void testRunnable() throws Exception {
        
        Try<Class<Void>> run = Try.of(() -> System.out.println("Runnable"));
        assertTrue(run.isSuccess());
        assertEquals(Void.TYPE, run.get());

        run = Try.of(() -> {throw new FileNotFoundException();});
        assertTrue(run.isFailure());
        run.onFailure(e -> assertTrue(e instanceof FileNotFoundException));
        
        try {
            run.get();
            fail();
        } catch (RuntimeException e) {
            //expected
            //FileNotFoundException wrapped into RunException
            assertEquals(FileNotFoundException.class, e.getCause().getClass());
        }
    }
    
    @Test
    public void testPureRunnable() throws Exception {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Run runnable");
            }
        };
        assertTrue(Try.of(() -> r.run()).isSuccess());
    }
}
