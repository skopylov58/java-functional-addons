package com.github.skopylov58.functional;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Test;

public class IOTest {

//  @Test
//  public void testIO() throws Exception {
//    IO<String> readLine = IO.of(() -> readLine());
//    IO<Void> greet = readLine.flatMap(s -> IO.consume(s, this::sayHello));
//    greet.run();
//  }

  
  @Test
  public void testIO2() throws Exception {
    
    var io = getLine()
    .flatMap(s -> readFile(s))
    .flatMap(s -> printLine(s));
    
    io.run();
    
  }
  
  IO<String> getLine() {
    return () -> "foo";
  }
  
  IO<String> readFile(String fileName) {
    return () -> "bar";
  }
  
  IO<Void> printLine(String text) {
    return () -> {
      System.out.println(text);
      return null;
    };
  }
  
  void sayHello(String s) {
    System.out.println("Hello " + s);
  }

  String readLine0() {
    System.out.println("Enter line:");
    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
    try {
      return r.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        r.close();
      } catch (IOException e) {// ignore}
      }
    }

  }
}
