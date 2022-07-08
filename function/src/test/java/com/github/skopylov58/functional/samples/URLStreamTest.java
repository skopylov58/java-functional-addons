package com.github.skopylov58.functional.samples;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.Try;

/**
 * Example of converting list of strings to list of URLs
 * <p>
 * Given: list/array of string URLS
 * <p>
 * Required: list of URL objects 
 * 
 * 
 * @author skopylov@gmail.com
 *
 */

public class URLStreamTest {
    
    final static String goocom = "http://google.com";
    final static String[] urls = { "foo", goocom, "bar" };

    @Test
    public void testURLStream() {
        test(this::urlListWithTry, urls);
        test(this::urlListTraditional, urls);
    }

    void test(Function<String[], List<URL>> func, String [] param) {
        List<URL> list =  func.apply(param);
        assertEquals(1, list.size());
        URL url = list.get(0);
        assertEquals(goocom, url.toString());
        System.out.println(url);
    }
    
    private List<URL> urlListWithTry(String[] urls) {
        return Stream.of(urls).map(s -> Try.of(() -> new URL(s)))
                .map(t -> t.onFailure(e -> System.out.println(e.getMessage()) ))
                .flatMap(Try::stream)
                .collect(Collectors.toList());
    }

    private List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls).map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException me) {
                System.out.println(me.getMessage());
                return null;
            }
        }).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

}
