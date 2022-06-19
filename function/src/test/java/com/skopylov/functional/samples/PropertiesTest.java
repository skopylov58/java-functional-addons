package com.skopylov.functional.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import com.skopylov.functional.Try;

public class PropertiesTest {

    final static String fileName = "/a/b/c";
    
    @Test
    public void testProperties() {
        Properties props = fromFileTraditional(fileName);
        assertNotNull(props);
        assertEquals(0, props.size());

        props = fromFileWithTry(fileName);
        assertNotNull(props);
        assertEquals(0, props.size());
    }

    public Properties fromFileTraditional(String fileName) {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)) {
            p.load(in);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return p;
    }

    public Properties fromFileWithTry(String fileName) {
        Properties p = new Properties();
        try (var input = Try.of(() -> new FileInputStream(fileName))) {
            return input.map(in -> {p.load(in);return p;})
            .onFailure(System.out::println)
            .optional()
            .orElse(p);
        }
    }

}
