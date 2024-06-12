package com.github.skopylov58.functional.samples;

import static org.junit.Assert.*;
import org.junit.Test;

public class BuilderTest {
  
  

  
  @Test
  public void testName() throws Exception {
    
    var b = Person.builder()
        .setAddress("")
        .setName("")
        .build();
    
    Person person = Person.builder()
      .configure(c -> {
        c.address = "";
        c.name = "";
    }).build();
    
    
  }
  
  
  

}
