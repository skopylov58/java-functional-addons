package com.skopylov.functional.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.skopylov.functional.Try;
import com.skopylov.functional.Tuple;

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
        .peek(t -> t.onFailure(this::logError, e -> e instanceof NumberFormatException))
        .flatMap(Try::stream)  //stream for Failure is empty
        .collect(Collectors.toList());
    }

    List<Number> fromStringArrayWithTry0(String [] nums) {
        return Stream.of(nums)
        .filter(Objects::nonNull)
        .map(s -> Try.success(s).map(i-> Integer.valueOf(i) , (i, e) -> logError(e)))
        .flatMap(Try::stream)  //stream for Failure is empty
        .collect(Collectors.toList());
    }
    
    List<Number> fromStringArrayTraditional(String [] nums) {
        List<Number> res = new LinkedList<>();
        for (String s : nums) {
            if (s == null) {
                continue;
            }
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

    List<Number> fromStringArrayWithTuple(String [] nums) {
        return Stream.of(nums)
        .map(s -> new Tuple<String, Try<Integer>>(s,Try.of(() ->Integer.valueOf(s))))
        .collect(Collectors.groupingBy(t -> t.second.isSuccess()))
        .get(true)
        .stream()
        .map(t -> t.second.get())
        .collect(Collectors.toList());
    }

    
    
    @Test
    public void testAssign() {
        assertTrue(IOException.class.isAssignableFrom(FileNotFoundException.class));
        assertFalse(IOException.class.isAssignableFrom(IndexOutOfBoundsException.class));
    }

}
