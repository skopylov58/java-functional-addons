package com.skopylov.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

import org.junit.Test;

public class InterfacesTest {

    @Test
    public void testExceptionalSupplier() throws Exception {
        ExceptionalSupplier<Integer> s = () -> 1;
        Integer integer = s.get();
        assertEquals(Integer.valueOf(1), integer);

        ExceptionalSupplier<Integer> throwingRunTime = () -> {throw new NullPointerException();};
        try {
            throwingRunTime.get();
            fail();
        } catch (NullPointerException npe) {
            //expected
        }

        ExceptionalSupplier<Integer> throwingChecked = () -> {throw new FileNotFoundException();};
        try {
            throwingChecked.get();
            fail();
        } catch (UncheckedIOException re) {
            //wrapped into runtime exception
            assertEquals(FileNotFoundException.class, re.getCause().getClass());
        }
    }
    
    @Test
    public void testCast() throws Exception {
        Supplier<Integer> intSuppl = () -> 1;
        ExceptionalSupplier<Integer> ex = () -> intSuppl.get();
        foo(ex);
        
        foo(ExceptionalSupplier.checked(intSuppl));
        
        try {
            foo((ExceptionalSupplier<Integer>) intSuppl);
            fail();
        } catch (ClassCastException e) {
            //ok
        }
    }
    
    void foo(ExceptionalSupplier<Integer> suppl) throws Exception {
        Integer res = suppl.getWithException();
        System.out.println(res);
        
    }
    
    @Test
    public void testExceptionalFunction() {
        ExceptionalFunction<String, URL> newUrl = URL::new;
        ExceptionalFunction<Integer, Integer> divideByZero = i -> i / 0;
        
        URL url = newUrl.apply("http://www.hpe.com");  //should be fine
        
        try { 
            newUrl.apply("foo/bar");
            fail();
        } catch (UncheckedIOException re) {
            //wrapped into runtime exception
            assertEquals(MalformedURLException.class, re.getCause().getClass());
        }
        
        try {
            divideByZero.apply(1);
            fail();
        } catch (ArithmeticException ae) {
            //expected
        }
    }
    
    @Test
    public void testExceptionalPredicate() {
        ExceptionalPredicate<Integer> even = i -> i % 2 == 0;
        
        assertTrue(even.test(4));
        
        ExceptionalPredicate<Integer> throwingRuntime = i -> {throw new NullPointerException();};
        try {
            throwingRuntime.test(1);
            fail();
        } catch (NullPointerException npe) {
            //expected
        }

        ExceptionalPredicate<Integer> throwingChecked = i -> {throw new FileNotFoundException();};
        try {
            throwingChecked.test(1);
            fail();
        } catch (UncheckedIOException re) {
            //wrapped into runtime exception
            assertEquals(FileNotFoundException.class, re.getCause().getClass());
        }
    }
    

    @Test
    public void testExceptionalConsumer() {
        ExceptionalConsumer<Integer> c = i -> System.out.println(i);
        c.accept(1);

        ExceptionalConsumer<Integer> throwingRuntime = i -> {throw new NullPointerException();};
        try {
            throwingRuntime.accept(1);
            fail();
        } catch (NullPointerException npe) {
            //expected
        }
        
        ExceptionalConsumer<Integer> throwingChecked = i -> {throw new FileNotFoundException();};
        try {
            throwingChecked.accept(1);
            fail();
        } catch (UncheckedIOException re) {
            //wrapped into runtime exception
            assertEquals(FileNotFoundException.class, re.getCause().getClass());
        }
    }

    @Test
    public void testExceptionalRunnable() {
        ExceptionalRunnable c = () -> System.out.println("run");
        c.run();

        ExceptionalRunnable throwingRuntime = () -> {throw new NullPointerException();};
        try {
            throwingRuntime.run();
            fail();
        } catch (NullPointerException npe) {
            //expected
        }
        
        ExceptionalRunnable throwingChecked = () -> {throw new FileNotFoundException();};
        try {
            throwingChecked.run();
            fail();
        } catch (UncheckedIOException re) {
            //wrapped into runtime exception
            assertEquals(FileNotFoundException.class, re.getCause().getClass());
        }
    }
    
}
