package com.github.skopylov58.retry;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
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
        
        future = Retry.of(() -> 1).retry();
        assertEquals(1, future.get().intValue());
    }

    @Test
    public void testSqlConnection() throws Exception {
        String url = "foo";
        CompletableFuture<Connection> future = Retry.of(() -> getConnection(url))
                .withBackoff(Retry.maxRetriesWithFixedDelay(5, Duration.ofMillis(10)))
                .retry();

        future.whenComplete((c, t) -> {assertNull(c); assertNotNull(t);});
        
        Thread.sleep(200);
    }
    
    @Test
    public void testCancel() throws Exception {
        
        var handler = Retry.foreverWithFixedDelay(Duration.ofMillis(50));
        
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .withBackoff(handler)
                .retry();
        Thread.sleep(200);
        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
        try {
            future.get();
            fail();
        } catch (CancellationException e) {
            //ok
        }
    }

    @Test
    public void testForever() throws Exception {
        var handler = Retry.foreverWithFixedDelay(Duration.ofMillis(50));

        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .withBackoff(handler)
                .retry();
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch(TimeoutException te) {
            //ok
        }
    }

    @Test
    public void testExecutor() throws Exception {
        CompletableFuture<Connection> future = 
                Retry.of(() -> getConnection("foo"))
                .withExecutor(Executors.newFixedThreadPool(1))
                .retry();
    }
    
    Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }
    
    @Test
    public void testWith() throws Exception {
        
        var res = Retry.of(() -> 1)
        .withBackoff(Retry.maxRetriesWithBinaryExponentialDelay(10, Duration.ofMillis(50), Duration.ofSeconds(1)))
        .retry();
        
    }
    
    
    
}
