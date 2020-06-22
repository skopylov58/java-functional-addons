package com.github.skopylov;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Single class utility to {@link #retry} given {@link java.util.function.Supplier} 
 * in asynchronous way using only {@link java.util.concurrent.CompletableFuture}.
 * 
 * @author Sergey Kopylov
 * @author skopylov@gmail.com
 *
 * @param <T> supplier's type
 */
public class Retrier<T> {
    
    private final Supplier<T> supplier; //Supplier to retry
    private final long maxTries;        //Maximum number of tries
    private final long delay;           //Delay between attempts
    private final TimeUnit timeUnit;    //Delay time unit

    private long currentTry = 0;
    private CompletableFuture<T> result;
    
    /**
     * Retrier constructor. 
     * @param supplier supplier to retry on.
     * @param maxTries maximum tries, zero or negative means retry forever. 
     * @param delay delay between retry attempts. 
     * @param timeUnit delay time unit.
     */
    public Retrier(Supplier<T> supplier, long maxTries, long delay, TimeUnit timeUnit) {
        this.supplier = supplier;
        this.maxTries = maxTries;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }
    
    /**
     * Performs asynchronous retry of given supplier.
     * See also constructor {@link #Retrier(Supplier, long, long, TimeUnit)}}
     * @return CompletableFuture
     */
    public CompletableFuture<T> retry() {
        result = new CompletableFuture<>();
        tryOnce();
        return result;
    }

    private void tryOnce() {
        CompletableFuture.supplyAsync(supplier)
        .handle(this::handle);
    }

    private T handle(T res, Throwable t) {
        if (t != null) {
            if (currentTry++ < maxTries || maxTries <= 0) {
                CompletableFuture.delayedExecutor(delay, timeUnit)
                .execute(this::tryOnce);
            } else {
                result.completeExceptionally(t);
            }
        } else {
            result.complete(res);
        }
        return res;
    }
}
