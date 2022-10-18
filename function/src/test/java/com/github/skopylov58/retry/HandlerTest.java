package com.github.skopylov58.retry;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.junit.Test;

public class HandlerTest {
    
    Duration min = Duration.ofMillis(50);
    Duration max = Duration.ofSeconds(1);
    
    @Test
    public void testName() throws Exception {
        
        var handler = Retry.Handler
        .withInterval(count -> Retry.exponentialBackoff(count, min, max, 2))
        .withCounter(count -> count < 100)
        .withException(ex -> ex instanceof IOException)
        .also((c, ex) -> System.out.println(ex.toString()));
     
        Optional<Duration> duration = handler.handle(0, new NullPointerException());
        assertTrue(duration.isEmpty());
        
    }
    
    @Test
    public void testExpBackoff() throws Exception {
        for (int i = 0; i < 50; i++) {
            Duration duration = Retry.exponentialBackoff(i, min, max, 2);
            System.out.println(duration.toMillis());
        }
    }

}
