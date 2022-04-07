package com.skopylov.ftry.samples;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import com.skopylov.ftry.TestBase;
import com.skopylov.ftry.Try;

/**
 * Example which reads the text from the URL.
 * @author sergey.kopylov@hpe.com
 *
 */
public class ReadURLTest {

    String urlString = "http://www.google.com/";

    @Test
    public void readURLTraditional() {
        List<String> list = readURLTraditional(urlString);
        System.out.println(list);
    }

    @Test
    public void readURLWithTry() {
        List<String> list = readURLWithTry(urlString);
        System.out.println(list);
    }

    public List<String> readURLTraditional(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            TestBase.logException(e);
            return Collections.emptyList();
        }
        try (InputStream inputStream = url.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            TestBase.logException(e);
            return Collections.emptyList();
        }
    }

    public List<String> readURLWithTry(String urlString) {
        return Try.of(() -> new URL(urlString))
                .map(URL::openStream)
                .map(i -> new BufferedReader(new InputStreamReader(i))).autoClose()
                .map(BufferedReader::lines)
                .logException()
                .map(s -> s.collect(Collectors.toList()))
                .getOrDefault(Collections.emptyList());
    }

    @Test
    public void testURL() throws Exception {
        Optional<URL> optional = Try.of(() -> new URL("foo.bar")).optional();
        assertTrue(optional.isEmpty());
    }

}
