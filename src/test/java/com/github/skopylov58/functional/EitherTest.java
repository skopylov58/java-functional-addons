package com.github.skopylov58.functional;

import static com.github.skopylov58.functional.Either.catching;

import java.net.Socket;
import java.net.URL;

import org.junit.Test;

public class EitherTest {
    
    @Test
    public void testEitherWithException() throws Exception {
        var either = Either.catching(() -> new URL("foo"));
        either.map(u -> u.getFile())
        .filter(n -> !n.isEmpty(), f -> new Exception());
    }
    
    @Test
    public void testEitherWithLeftString() throws Exception {
        var either = Either.catching(
                () -> new URL("foo"),
                e -> e.getMessage());
        
        either.map(u -> u.getFile())
        .filter(n -> !n.isEmpty(), f -> "Empty file");
    }
    
    @Test
    public void testSocket() throws Exception {
        
        catching(() -> new Socket("foo", 1234))
        .flatMap(catching(Socket::getOutputStream))
        .flatMap(catching(o -> {
            o.write(new byte[] {1,2,3});
        }));
        
    }
    
}
