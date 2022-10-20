package com.github.skopylov58.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.Test;

public class RetrySupplierTest {

    @Test
    public void testFailure() throws Exception {
        //Not enough num of tries
        FailingOp op = new FailingOp(5);
        CompletableFuture<Long> future = Retry.of(() -> op.get())
        .withBackoff(Retry.maxRetriesWithFixedDelay(4, Duration.ofMillis(100)))
        .retry();
        
        try {
            Long res = future.get();
            fail();
        } catch ( ExecutionException e) {
            //ok
        }
    }
    
    @Test
    public void testSuccess() throws Exception {
        FailingOp op = new FailingOp(5);
        CompletableFuture<Long> future = Retry.of(() -> op.get())
        .withBackoff(Retry.maxRetriesWithFixedDelay(6, Duration.ofMillis(100)))
        .retry();
        
        long res = future.get();
        assertEquals(6, res);
    }


    @Test
    public void testSuccessWithBadHandler() throws Exception {
        BiFunction<Long, Throwable, Duration> throwingHandler = (i,t) -> {throw new IllegalStateException();};
        FailingOp op = new FailingOp(5);
        CompletableFuture<Long> future = Retry.of(() -> op.get())
        .withBackoff(throwingHandler)
        .retry();
        
        try {
            long res = future.get();
            fail();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static class FailingOp implements Supplier<Long> {
        
        private final long maxTries;
        private long currentTry = 0;

        public FailingOp(long maxTries) {
            this.maxTries = maxTries;
        } 
        
        @Override
        public Long get() {
            if (currentTry++ < maxTries) {
                throw new IllegalStateException();
            }
            return currentTry;
        }
    }

}
