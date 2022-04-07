package com.skopylov.ftry.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.skopylov.ftry.Try;

/**
 * Converts list of strings to numbers.
 * @author skopylov@gmail.com
 *
 */
public class NumbersTest {

    String[] numbers = { "1", "2", "3", "z" };

    @Test
    public void testNumbers() {
        test(this::fromStringArrayTraditional, numbers);
        test(this::fromStringArrayWithTry, numbers);
    }
    
    void test(Function<String[], List<Number>> func, String [] param) {
        List<Number> res =  func.apply(param);
        assertEquals(3, res.size());
        assertEquals(1, res.get(0));
        System.out.println(res);
    }

    List<Number> fromStringArrayWithTry(String [] nums) {
        return Stream.of(nums)
        .map(s -> Try.of(() ->Integer.valueOf(s)))
        .peek(Try::logException)
        .flatMap(Try::stream)  //stream for Failure is empty
        .collect(Collectors.toList());
    }

    List<Number> fromStringArrayTraditional(String [] nums) {
        List<Number> res = new LinkedList<>();
        for (String s : nums) {
            try {
                Number n = Integer.valueOf(s);
                res.add(n);
            } catch (NumberFormatException e) {
                logError(e);
            }
        }
        return res;
    }
    
    void logError(Exception e) {
        System.out.println(e.getClass().getName() + " " + e.getMessage());
    }
    
    @Test
    public void testAssign() {
        assertTrue(IOException.class.isAssignableFrom(FileNotFoundException.class));
        assertFalse(IOException.class.isAssignableFrom(IndexOutOfBoundsException.class));
    }

}
