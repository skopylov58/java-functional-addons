package com.github.skopylov;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.skopylov.functional.Try;

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

        Try<Connection> tr = Try.of(future);
        assertTrue(tr.isFailure());
        assertEquals(5, h.getCounter());
    }
    
    @Test
    public void testForever() throws Exception {
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .forever()
                .delay(100, TimeUnit.MILLISECONDS)
                .retry();
        
        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
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
    
    
    Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
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
