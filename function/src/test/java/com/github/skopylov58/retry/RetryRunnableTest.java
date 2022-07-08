package com.github.skopylov58.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.skopylov58.retry.Retry;


public class RetryRunnableTest {


    @Test
    public void testFailure() throws Exception {
        //Number of tries is not enough
        FailingRunnable r = new FailingRunnable(5);
        CompletableFuture<Void> future = Retry.of(r)
                .maxTries(4)
                .delay(100, TimeUnit.MILLISECONDS)
                .retry();

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
        CompletableFuture<Void> future = Retry.of(r)
                .maxTries(5)
                .delay(100, TimeUnit.MILLISECONDS)
                .retry();

        future.get();
        assertEquals(5, r.getCounter());
    }

    
    static class FailingRunnable implements Runnable {
        
        private final long maxTries;
        private long currentTry = 0;

        public FailingRunnable(long maxTries) {
            this.maxTries = maxTries;
        } 
        
        @Override
        public void run() {
            currentTry++;
            if (currentTry < maxTries) {
                throw new IllegalStateException();
            }
            System.out.println("success with " + currentTry);
        }
        
        public long getCounter() {
            return currentTry;
        }
    }
    
}
