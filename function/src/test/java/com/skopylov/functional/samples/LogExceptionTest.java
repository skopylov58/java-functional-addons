package com.skopylov.functional.samples;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

import com.skopylov.functional.Try;

public class LogExceptionTest {
    
    
    @Test
    public void testName() throws Exception {
        
        InputStream input = Try.<InputStream>of(() -> new FileInputStream("/a/b/c"))
                .autoClose()
                //.logException()
                .onFailure(this::logError)
                .orElse(new ByteArrayInputStream("foo".getBytes()));
    }
    
    void logError(Exception e) {
        e.printStackTrace();
    }
    

}
