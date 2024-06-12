package com.github.skopylov58.functional;

import java.time.Duration;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.LongStream;
import org.junit.Test;
import com.github.skopylov58.functional.FPUtils.Backoff;

public class RetryTest {
  
  
  @Test
  public void testExponentialBackoff() {
    
//    Backoff backoff = exponentialBackoff(10, 1000)
//        .withJitter(simpleJitter(Duration.ofMillis(40)));
//    System.out.println(backoff);

    Backoff exponentialBackoff = FPUtils.exponentialBackoff(10, 1000);
    for (long i = 0; i < 35; i++) {
      Duration dur = exponentialBackoff.apply(i);
      System.out.println(dur);
    }
  }

  
  Duration jitter(Duration d, Random r) {
    long rand = r.nextLong(0, d.toMillis());
    return Duration.ofMillis(rand);
  }
  
  @Test
  public void testWithJitter() throws Exception {
    Function<Long, Duration> fixedDelay = i -> Duration.ofHours(1);
    Function<Long, Duration> fixedDelayWithJitter = fixedDelay.andThen(d -> d.plus(jitter(d, new Random())));
    
    var res = LongStream.range(0, 20).mapToObj(i -> fixedDelayWithJitter.apply(i)).toList();
    System.out.println(res);
    
  }


}
