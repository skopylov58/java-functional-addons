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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.Test;

import com.github.skopylov58.functional.Try;
import com.github.skopylov58.functional.TryUtils;
import com.github.skopylov58.retry.Retry.Worker;

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
    public void testRetry() throws Exception {
        Executor ex = Executors.newSingleThreadExecutor();
        var dur = TryUtils.measure(() -> {
            IntStream.range(0, 1_000)
            .mapToObj(i -> this.retryConnection(ex))
            .toList()
            .forEach(f -> Try.of(()->f.get()));
        });
        System.out.println(dur);
    }

    CompletableFuture<Connection> retryConnection(Executor ex) {
        return Retry.of(() -> DriverManager.getConnection("jdbc:foo"))
            .withFixedDelay(Duration.ofMillis(100))
            .withExecutor(ex)
            .retryOnValue(Objects::isNull)
            .retry(10);
    }
    
    Connection retryConnection() {
        return TryUtils.retry(10, Duration.ofMillis(100), () -> DriverManager.getConnection("jdbc:foo"));
    }

    
    Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Test
    public void testNaiveRetry() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);
        var dur = TryUtils.measure(() -> {
            IntStream.range(0, 12)
            .mapToObj(i -> ex.submit(() -> this.retryConnection()))
            .toList()
            .forEach(f -> Try.of(()->f.get()));
        });
        System.out.println(dur);
    }

    int [] fibonacci = {1,1,2,3,5,8,13,21,34};
    Duration fibonacciBackoff(int i, Duration min, Duration max) {
        if (i >= fibonacci.length) {
            return max;
        } else {
            var d = min.multipliedBy(fibonacci[i]);
            return d.compareTo(max) > 0 ? max : d;
        }
    }
}
