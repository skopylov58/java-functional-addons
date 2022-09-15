package com.github.skopylov58.functional;

import java.util.Date;

import org.junit.Test;

public class TupleTest {

    @Test
    public void test() throws Exception {
        Tuple3<Integer, String, Date> t = new Tuple3<>(1, "foo", new Date());
        String string = t.toString();
        System.out.println(string);
    }
    
    @Test
    public void testNull() throws Exception {
        Tuple3<Integer, String, Date> t = new Tuple3<>(null, null, null);
        String string = t.toString();
        System.out.println(string);
    }

    
}
