package com.skopylov.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class RetryTest {
    
    @Test
    public void testInt() throws Exception {
        CompletableFuture<Integer> future = Retry.of(() -> 1).retry();
        assertEquals(1, future.get().intValue());
        
        future = Retry.of(() -> 1).maxTries(10).retry();
        assertEquals(1, future.get().intValue());
    }

    @Test
    public void testSqlConnection() throws Exception {
        String url = "foo";
        CountingErrorHandler h = new CountingErrorHandler();
        CompletableFuture<Connection> future = Retry.of(() -> getConnection(url))
                .maxTries(5)
                .delay(100, TimeUnit.MILLISECONDS)
                .withErrorHandler(h)
                .retry();

        future.whenComplete((c, t) -> {assertNull(c); assertNotNull(t);});
    }
    
    @Test
    public void testForever() throws Exception {
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .forever()
                .delay(500, TimeUnit.MILLISECONDS)
                .withErrorHandler((i, max, t) -> System.out.println(t.getMessage()))
                .retry();
        Thread.sleep(3000);
        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
        try {
            future.get();
            fail();
        } catch (CancellationException e) {
            //ok
        }
        //Thread.sleep(10000000);
    }

    @Test
    public void testForever2() throws Exception {
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .forever()
                .delay(500, TimeUnit.MILLISECONDS)
                .withErrorHandler((i, max, t) -> System.out.println(t.getMessage()))
                .retry();
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch(TimeoutException te) {
            //ok
        }
        
        //Thread.sleep(10000000);
    }

    
    
    @Test
    public void testExecutor() throws Exception {
        CompletableFuture<Connection> future = 
                Retry.of(() -> getConnection("foo"))
                .maxTries(10)
                .delay(10, TimeUnit.MILLISECONDS)
                .withExecutor(Executors.newFixedThreadPool(1))
                .retry();
    }
    
    
    Connection getConnection(String url) {
        try {
            return DriverManager.getConnection(url);
        } catch(SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    static class CountingErrorHandler implements Retry.ErrorHandler {
        private long counter = 0;
        long getCounter() {
            return counter;
        }
        @Override
        public void handle(long currentTry, long maxTries, Throwable exception) {
            counter++;
            System.out.println("Try # " + currentTry + "/" + maxTries + " " + exception.getCause());
        }
    }
    

    
    
}
