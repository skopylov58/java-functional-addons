package com.github.skopylov58.functional;

import static com.github.skopylov58.functional.TryUtils.throwingMapper;
import static com.github.skopylov58.functional.TryUtils.toEither;
import static com.github.skopylov58.functional.TryUtils.toFuture;
import static com.github.skopylov58.functional.TryUtils.toOptional;
import static com.github.skopylov58.functional.TryUtils.toResult;
import static com.github.skopylov58.functional.TryUtils.toResultJava8;
import static com.github.skopylov58.functional.TryUtils.memoize;
import static com.github.skopylov58.functional.TryUtils.ctou;
import static java.util.function.Predicate.not;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.TryUtils.Result;

public class TryUtilsTest {

    final List<String> URLS = List.of("www.google.com", "foo.bar", "http://a.b.c");
    final String[] NUMS = new String[]{"foo", "bar", "3", "4.9"};
    
    @Test
    public void testWithOptional() throws Exception {
        List<URL> list = URLS.stream()
        .map(toOptional(URL::new))
        .flatMap(Optional::stream)
        .toList();
        check(list);
    }

    void check(List<URL> list) throws MalformedURLException {
        System.out.println(list);
        assertTrue(list.size() == 1);
        assertTrue(list.get(0).equals(new URL("http://a.b.c")));
    }
    
    @Test
    public void testWithEither() throws Exception {
        List<URL> res = URLS.stream()
            .map(toEither(URL::new))
            .peek(either -> either.accept(System.out::println, r -> {}))
            .flatMap(Either::stream)
            .toList();
        check(res);
    }

    @Test
    public void testWithResultJava8() throws Exception {
        List<URL> res = URLS.stream()
            .map(toResultJava8(URL::new))
            .peek(r -> {if (r.failed()) System.out.println(r.cause());})
            .filter(Predicate.not(TryUtils.ResultJava8::failed))
            .map(r -> r.result())
            .toList();
        check(res);
    }

    @Test
    public void testWithResultRecord() throws Exception {
        var res = URLS.stream()
            .map(toResult(URL::new))
            .filter(rec -> !rec.isFailure())
            .map(Result::result)
            .toList();
        
        check(res);
    }

    @Test
    public void intList() {
        var list = Stream.of(NUMS)
        .map(toOptional(Integer::parseInt)) //Runtime NumberFormatException may happen here
        .flatMap(Optional::stream)
        .toList();
    }

    @Test
    public void intListWithResult() throws NumberFormatException {
        var list = Stream.of(NUMS)
        .map(toResult(Integer::parseInt)) //Runtime NumberFormatException may happen here
        .peek(this::handleErr) 
        .flatMap(Result::stream)
        .toList();
    }
    
    @Test
    public void intListWithNumberFormat() {
        NumberFormat format = NumberFormat.getInstance();
        var list = Stream.of(NUMS)
        .map(toOptional(format::parse)) //Checked ParseException may happen here
        .flatMap(Optional::stream)
        .map(Number::intValue)
        .toList();
        System.out.println(list);
    }
    
    void handleErr(Result r) {
        if (r.isFailure()) {
            System.out.println(r.exception());
        }
    }

    List<Number> intListWithResult(String [] numbers) {
        NumberFormat format = NumberFormat.getInstance();
        return Stream.of(numbers)      //Stream<String>
        .map(toResult(format::parse))  //Stream<Result<Number>>, ParseException may happen
        .peek(this::handleErr)         //Stream<Result<Number>>
        .flatMap(Result::stream)       //Stream<Number>
        .toList();                     //List<Number>
    }

    List<Number> intListWithFuture(String [] numbers) {
        NumberFormat format = NumberFormat.getInstance();
        return Stream.of(numbers)      //Stream<String>
        .map(toFuture(format::parse))  //Stream<CompletableFuture<Number>>, ParseException may happen
        .filter(not(CompletableFuture::isCompletedExceptionally))
        .map(CompletableFuture::join)
        .toList();                     //List<Number>
    }
    
    @Test
	public void testCurrying() throws Exception {
    	Function<String,Integer> handled = TryUtils.catchingMapper(Integer::parseInt, (t,e) -> -1);
	}
    
    @Test
    public void testIgnoreErrors() {
    	var list = Stream.of("1", "foo", "2", "bar")
		.map(toOptional(Integer::valueOf))
		.flatMap(Optional::stream)
		.toList();
    	
    	 assertThat(List.of(1, 2), is(list));
    }
    
    @Test
    public void testIgnoreErrorsAndLog() {
    	var list = Stream.of("1", "foo", "2", "bar")
		.map(toOptional(Integer::valueOf, (i, e) -> System.err.println(i + " causes error " + e.getMessage())))
		.flatMap(Optional::stream)
		.toList();

		assertThat(List.of(1, 2), is(list));
    }
    
    @Test
    public void testProvideDefaultsForErrors() {
    	var list = Stream.of("1", "foo", "2", "bar")
		.map(toOptional(Integer::valueOf))
		.map(o -> o.orElse(-1))
		.toList();
    	
		assertThat(List.of(1, -1, 2, -1), is(list));
    }
    
    @Test
    public void testHandleErrors() {
    	var list = Stream.of("1", "foo", "2", "foo-bar")
		.map(TryUtils.catchingMapper(Integer::valueOf, (s, e) -> s.length()))
		.toList();
    	
    	assertThat(List.of(1, 3, 2, 7), is(list));
    }

    @Test(expected = NumberFormatException.class)
    public void testStopOnErrors() {
    	Stream.of("1", "foo", "2", "bar")
		.map(throwingMapper(Integer::valueOf))
		.toList();
    	
    	fail();
    }
    
    @Test
	public void testMemoize() throws Exception {
    	
    	Function<String, String> hello = s -> {
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		return "Hello " + s;
    	};
    	
    	var cache = new ConcurrentHashMap<String, String>();
    	Function<String,String> memoizedHello = memoize(hello, cache);
    	
    	System.out.println("Start");
    	String apply = memoizedHello.apply("Sergey");
    	System.out.println(apply);
    	

    	apply = memoizedHello.apply("Sergey");
    	System.out.println(apply);
	}
    
    
    @Test
	public void testDecorator() throws Exception {
    	
    	Function<String, String> hello = s -> "Hello " + s;
    	
    	UnaryOperator<String> before = ctou(s -> {System.out.println("before " + s);}); 
    	UnaryOperator<String> after  = ctou(s -> {System.out.println("after " + s);}); 
    	
    	Function<String, String> decoratedHello = hello.compose(before).andThen(after);
    	
    	String h = decoratedHello.apply("Sergey");
    	System.out.println(h);
	}
    
    
}
