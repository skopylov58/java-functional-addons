package com.github.skopylov;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

public class RetrierTest {
    
    @Test
    public void testSimple() throws Exception {
        Retrier<Long> r = new Retrier<>(() -> 1L, 10, 100, TimeUnit.MILLISECONDS);
        r.retry().thenAccept(i -> assertEquals(1L, i.longValue())).get();
    }
    
    @Test
    public void testFailingOpSuccess() throws Exception {
        FailingOp f = new FailingOp(5);
        Retrier<Long> r = new Retrier<>(f, 10, 100, TimeUnit.MILLISECONDS);
        r.retry().thenAccept(i -> assertEquals(5L, i.longValue())).get();
    }
    
    @Test
    public void testFailingOpFail() throws Exception {
        FailingOp f = new FailingOp(5);
        Retrier<Long> r = new Retrier<>(f, 4, 100, TimeUnit.MILLISECONDS);
        Long res = r.retry().exceptionally(t -> -99L).get();
        assertEquals(-99L, res.longValue());
    }
    
    //Succeeds with maxTries 
    static class FailingOp implements Supplier<Long> {
        
        private final long maxTries;
        private long currentTry = 0;

        public FailingOp(long maxTries) {
            this.maxTries = maxTries;
        } 
        
        @Override
        public Long get() {
            if (currentTry < maxTries) {
                currentTry++;
                throw new IllegalStateException();
            }
            return currentTry;
        }
    }

}
