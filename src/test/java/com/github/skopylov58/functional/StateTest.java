package com.github.skopylov58.functional;

import static org.junit.Assert.*;
import java.util.Objects;
import org.junit.Test;

public class StateTest {


  @Test
  public void testName() throws Exception {
    State<String, Integer> state = State.pure("foo");
    
//    var run = state.apply(1);
//    System.out.println(run);
    

    var state2 = state.flatMap(s -> {
      return i -> new Tuple<>(s + ", bar", i + 1);
    });
    
//    System.out.println(state2.apply(1));

    var state3 = state2.flatMap(s -> {
      return i -> new Tuple<>(s + ", zoo", i + 1);
    });
    
    
    Tuple<String,Integer> apply = state3.apply(1);
    System.out.println(apply);
    

    
  }
  
}
