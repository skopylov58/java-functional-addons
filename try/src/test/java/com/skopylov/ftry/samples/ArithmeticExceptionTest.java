package com.skopylov.ftry.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.skopylov.ftry.Try;

public class ArithmeticExceptionTest {
    
    /**
     * @param x
     * @param y
     * @return x/y
     * @throws ArithmeticException if y == 0
     */
    int div(int x, int y) {
        return x / y;
    }

    /**
     * Recovers ArithmeticException
     * 
     * @param x
     * @param y
     * @return x/y or Integer.MAX_VALUE or Integer.MIN_VALUE in case of {@link ArithmeticException}
     */
    int divWithRecover(int x, int y) {
        return Try.of(() -> div(x, y))
                .recover(() -> x > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE, ArithmeticException.class)
                .get();
    }

    @Test
    public void testDivByZero() {

        assertEquals(2, div(4, 2));

        try {
            div(2, 0);
            fail();
        } catch (ArithmeticException e) {
            // expected
        }

        assertEquals(2, divWithRecover(4, 2));
        assertEquals(Integer.MAX_VALUE, divWithRecover(1, 0));
        assertEquals(Integer.MIN_VALUE, divWithRecover(-1, 0));
    }
}
