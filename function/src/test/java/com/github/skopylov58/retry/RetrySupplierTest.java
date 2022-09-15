package com.github.skopylov58.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

public class RetrySupplierTest {

    @Test
    public void testFailure() throws Exception {
        //Not enough num of tries
        FailingOp op = new FailingOp(5);
        CompletableFuture<Long> future = Retry.of(() -> op.get())
        .maxTries(4)
        .delay(100, TimeUnit.MILLISECONDS)
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
        .maxTries(6)
        .delay(100, TimeUnit.MILLISECONDS)
        .retry();
        
        long res = future.get();
        assertEquals(5, res);
    }


    @Test
    public void testSuccessWithBadErrorHandler() throws Exception {
        //Make sure bad error handler will not spoil result
        Retry.ErrorHandler throwingHandler = (i,max,t) -> {throw new IllegalStateException();};
        FailingOp op = new FailingOp(5);
        CompletableFuture<Long> future = Retry.of(() -> op.get())
        .maxTries(6)
        .delay(100, TimeUnit.MILLISECONDS)
        .withErrorHandler(throwingHandler)
        .retry();
        
        long res = future.get();
        assertEquals(5, res);
    }

    
    static class FailingOp implements Supplier<Long> {
        
        private final long maxTries;
        private long currentTry = 0;

        public FailingOp(long maxTries) {
            this.maxTries = maxTries;
        } 
        
        @Override
        public Long get() {
            currentTry++;
            if (currentTry < maxTries) {
                throw new IllegalStateException();
            }
            return currentTry;
        }
    }

}
