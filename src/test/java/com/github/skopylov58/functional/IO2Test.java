package com.github.skopylov58.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Properties;
import org.junit.Test;

public class IO2Test {

  @Test
  public void testCombineMonads() {

    IO2<Properties, Integer> ioi = ctc -> Option.some(3);
    IO2<Properties, String>  ios = ctc -> Result.success("three");

    IO2<Properties,String> combined = ioi.flatMap(i -> ios.map(s -> s + " " + i));

    Monad<String> result = combined.run(new Properties());
    System.out.println(result);

    boolean isSuccess = result instanceof Result.Success;
    assertTrue(isSuccess);

    String str = result.getOrDefault(null);
    assertEquals("three 3", str);

  }

  @Test
  public void testAccessContext() {

    IO2<Properties, Integer> ioi = ctx -> Option.some(3);

    IO2<Properties, String> strio = ioi.flatMap( i -> {
      return ctx -> {
        String prop = ctx.getProperty(i.toString());
        return Option.some(prop);
      };
    });

    Properties p = new Properties();
    p.put("3", "three");

    Monad<String> resMonad = strio.run(p);

    System.out.println(resMonad);


  }

  @Test
  public void testVoid() {
    var x = IO2.of(5);
    x = x.map(i -> i + 2);
    var res = x.run();

    assertEquals(Integer.valueOf(7) , res.getOrDefault(0));
    System.out.println("res = " + res);
  }

}
