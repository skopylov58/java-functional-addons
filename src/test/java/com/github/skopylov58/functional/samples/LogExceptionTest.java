package com.github.skopylov58.functional.samples;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

import com.github.skopylov58.functional.Try;

public class LogExceptionTest {
    
    
    @Test
    public void testName() throws Exception {
        
        InputStream input = Try.<InputStream>of(() -> new FileInputStream("/a/b/c"))
                .onFailure(this::logError)
                .optional()
                .orElse(new ByteArrayInputStream("foo".getBytes()));
    }
    
    void logError(Exception e) {
        e.printStackTrace();
    }
    

}
