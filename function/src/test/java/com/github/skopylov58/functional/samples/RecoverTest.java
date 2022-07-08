package com.github.skopylov58.functional.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.ExceptionalSupplier;
import com.github.skopylov58.functional.Try;

public class RecoverTest {
    
    @Test
    public void testName() throws Exception {
        Optional<String> optional = getPropertyWithTry("/a/b/c", "propName");
        assertTrue(optional.isEmpty());
        
        optional = getPropertyWithTryAndStream("/a/b/c", "propName");
        assertTrue(optional.isEmpty());
    }

    @Test
    public void testName1() throws Exception {
        
        System.setProperty("propName", "foo");
        
        Optional<String> optional = getPropertyWithTry("/a/b/c", "propName");
        assertEquals("foo", optional.get());
        
        optional = getPropertyWithTryAndStream("/a/b/c", "propName");
        assertEquals("foo", optional.get());
        
        System.clearProperty("propName");
        
    }

    Optional<String> getPropertyWithTry(String fileName, String propName) {
        return Try.of(() -> readPropertyFromFile(fileName, propName))
        .onFailure(System.err::println)
        .filter(Objects::nonNull)
        .recover(() -> readPropertyFromFile(System.getProperty("user.home")+"/"+ fileName, propName))
        .onFailure(System.err::println)
        .filter(Objects::nonNull)
        .recover(() -> System.getenv(propName))
        .filter(Objects::nonNull)
        .recover(() -> System.getProperty(propName))
        .filter(Objects::nonNull)
        .optional();
    }
    
    Optional<String> getPropertyWithTryAndStream(String fileName, String propName) {
        return Stream.of((ExceptionalSupplier<String>) 
            () -> readPropertyFromFile(fileName, propName),
            () -> readPropertyFromFile(System.getProperty("user.home")+"/"+ fileName, propName),
            () -> System.getenv(propName),
            () -> System.getProperty(propName))
        .map(Try::of)
        .peek(t -> t.onFailure(System.err::println))
        .flatMap(Try::stream)
        .filter(Objects::nonNull)
        .findFirst();
    }
    
    Optional<String> getPropertyTraditional(String fileName, String propName) {
        String res = null;
        String [] files = new String[] {fileName, System.getProperty("user.home")+"/"+ fileName};
        for (int i = 0; i < files.length; i++) {
            try {
                res = readPropertyFromFile(files[i], propName);
                if (res != null) {
                    return Optional.of(res);
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        
        res = System.getenv(propName);
        if (res != null) {
            return Optional.of(res);
        }
        res = System.getProperty(propName);
        if (res != null) {
            return Optional.of(res);
        }
        return Optional.empty();
    }
    
    String readPropertyFromFile(String fileName, String propName) throws FileNotFoundException, IOException {
        try(FileInputStream in = new FileInputStream(fileName)) {
            Properties p = new Properties();
            p.load(in);
            return p.getProperty(propName);
        }
    }

}
