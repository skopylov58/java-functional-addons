package com.github.skopylov58.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;


public class RetryRunnableTest {


    @Test
    public void testFailure() throws Exception {
        //Number of tries is not enough
        FailingRunnable r = new FailingRunnable(5);
        var future = Retry.of(() -> {r.run(); return null;})
                .withFixedDelay(Duration.ofMillis(100))
                .retry(4);
        
        try {
            future.get();
            fail();
        } catch (ExecutionException e ) {
            //ok
        }
    }

    @Test
    public void testSuccess() throws Exception {
        FailingRunnable r = new FailingRunnable(5);
        var future = Retry.of(() -> {r.run(); return null;})
                .withFixedDelay(Duration.ofMillis(100))
                .retry(6);

        future.get();
        assertEquals(6, r.getCounter());
    }

    
    static class FailingRunnable implements Runnable {
        
        private final long maxTries;
        private long currentTry = 0;

        public FailingRunnable(long maxTries) {
            this.maxTries = maxTries;
        } 
        
        @Override
        public void run() {
            if (currentTry++ < maxTries) {
                throw new IllegalStateException();
            }
            System.out.println("success with " + currentTry);
        }
        
        public long getCounter() {
            return currentTry;
        }
    }
    
}
