package com.github.skopylov58.retry;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Test;

import com.github.skopylov58.functional.TryUtils;

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
                .withFixedDelay(Duration.ofMillis(10))
                .retry(5);

        future.whenComplete((c, t) -> {assertNull(c); assertNotNull(t);});
        
        Thread.sleep(200);
    }
    
    @Test
    public void testCancel() throws Exception {
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .withFixedDelay(Duration.ofMillis(50))
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
        CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                .withFixedDelay(Duration.ofMillis(50))
                .retry();
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch(TimeoutException te) {
            //ok
        }
    }

    @Test
    public void testExecutor() throws Exception {
        
        var executor = Executors.newFixedThreadPool(1);
        List<CompletableFuture<Connection>> futures = new ArrayList<>();
        Duration dur = TryUtils.measure(() -> {
            for (int i = 0; i < 10_000; i++) {
                CompletableFuture<Connection> future = Retry.of(() -> getConnection("foo"))
                        .withFixedDelay(Duration.ofMillis(100))
                        .withExecutor(executor)
                        .retry(10);
                futures.add(future);
            }
            for (CompletableFuture<Connection> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    // System.out.println(e);
                }
            }
        });
        System.out.println(dur);
    }
    
    
    Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }
    
    @Test
    public void testExponent() throws Exception {
        for (int i = 0; i< 100; i++) {
            Duration d15 = exponentialBackoff(i, Duration.ofMillis(10), Duration.ofMillis(1000), 1.5);
            Duration d2 = exponentialBackoff(i, Duration.ofMillis(10), Duration.ofMillis(1000), 2);
            System.out.println(i + "\t" + d15.toMillis() + "\t " + d2.toMillis() );
        }
    }
    
    @Test
    public void testExponent2() throws Exception {
        Duration dur = exponentialBackoff(Long.MAX_VALUE, Duration.ofMillis(10), Duration.ofMillis(1000), Double.MAX_VALUE);
        assertEquals(1000, dur.toMillis());
    }
    
    void apiExperiment() {
        
        Predicate<Integer> intp = null;
        Predicate<Throwable> exp = null;
        Supplier<Duration> dursup = null;
        
        BiFunction<Integer, Throwable, Duration> bi = (i, t) -> {
            if (!intp.test(i)) {
                return null;
            }
            if (!exp.test(t)) {
                return null;
            }
            return dursup.get();
        };
    }
    
    /**
     * Exponential backoff function. 
     * <p>
     * delay<sub>i</sub> = initial * multFactor<sup>i</sup>
     * 
     * @param i try number starting with 0
     * @param initial initial retry interval
     * @param max maximum retry interval
     * @param multFactor the multiplicative factor or "base", 2 for binary exponential backoff
     * @return retry interval = initial * multFactor<sup>i</sup> or max
     */
    static Duration exponentialBackoff(long i, Duration initial, Duration max, double multFactor) {
        if (multFactor <= 1d) {
            throw new IllegalArgumentException("Expecting multFactor greater 1");
        }
        double factor = Math.pow(multFactor, i);
        if (Double.isNaN(factor) || Double.isInfinite(factor)) {
            return max;
        }
        double resDouble = factor * initial.toMillis();
        if (Double.isNaN(resDouble) || Double.isInfinite(resDouble)) {
            return max;
        }
        Duration res = Duration.ofMillis((long) resDouble);
        if (res.compareTo(max) > 0) {
            return max;
        } else {
            return res;
        }
    }
    
    
}
