package com.github.skopylov58.functional;

import static org.junit.Assert.assertTrue;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

public class ZIOTest {
    
    
    @Test
    public void test() {
        
        var zio = new ZIO<>((File f) -> Either.catching(() -> Files.lines(f.toPath()).toList()));

        Either<Exception,List<String>> runned = zio.run().apply(new File("."));
        
        System.out.println(runned);
        
        assertTrue(runned.isLeft());
        
            
    }
}

