package com.github.skopylov58.retry;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.skopylov58.retry.Retry;

/**
 * Example to compare Retry against traditional approach.
 * 
 * @author skopylov@gmail.com
 *
 */
public class RetryTraditionalTest {
    private static final int MAX_TRIES = 5;
    private static final int RETRY_DELAY = 100;
    
    @Test
    public void testTraditional() throws InterruptedException, ExecutionException {
        int r = retryTraditional();
        assertEquals(3, r);
    }
    
    @Test
    public void testWithRetry() throws InterruptedException, ExecutionException {
        int r = retryWithRetry();
        assertEquals(3, r);
    }

    public Integer retryWithRetry() throws InterruptedException, ExecutionException {
        IntProvider prov = new IntProvider();
        return Retry.of(() -> prov.getInt()).maxTries(MAX_TRIES)
        .delay(RETRY_DELAY, TimeUnit.MILLISECONDS)
        .retry()
        .get();
    }

    public Integer retryTraditional() throws InterruptedException, ExecutionException {
        IntProvider prov = new IntProvider();
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                return prov.getInt();
            } catch (Exception e) {
                System.out.println("Retry " + i + " failed with exception");
                if (i == MAX_TRIES - 1) { //last try
                    throw new ExecutionException("Failed after " + MAX_TRIES + " tries.", e);
                } else {
                    Thread.sleep(RETRY_DELAY);
                }
            }
        }
        throw new IllegalStateException("Should not be here");
    }

    static class IntProvider {
        private int counter = -1;
        Integer getInt() {
            counter++;
            if (counter < 3) {
                throw new IllegalStateException(String.valueOf(counter));
            } else {
                return counter;
            }
        }
    }
}
