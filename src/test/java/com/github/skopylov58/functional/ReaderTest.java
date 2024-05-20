package com.github.skopylov58.functional;

import static org.junit.Assert.*;
import java.util.Properties;
import java.util.function.Function;
import org.junit.Test;

public class ReaderTest {

  
  @Test
  public void testPropertiesReader() throws Exception {
    
    Properties props = new Properties();
    props.put("foo", "bar");

    Reader<Properties, String> r = Reader.pure("foo");
    
    Integer length = r.map(s -> "Hello " + s)
    .flatMap(s -> Reader.of(p -> strLength(s, p)))
    .apply(props);
    
    System.out.println("Length: " + length);
    assertTrue(9==length);
    
  }

  int strLength(String s, Properties p) {
    System.out.println("Accessing properties " + p);
    return s.length();
  }
  

}
