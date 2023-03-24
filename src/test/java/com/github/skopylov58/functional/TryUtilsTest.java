package com.github.skopylov58.functional;

import static com.github.skopylov58.functional.TryUtils.toEither;
import static com.github.skopylov58.functional.TryUtils.toOptional;
import static com.github.skopylov58.functional.TryUtils.toResult;
import static com.github.skopylov58.functional.TryUtils.toResultJava8;
import static java.util.function.Predicate.not;
import static com.github.skopylov58.functional.TryUtils.toFuture;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.TryUtils.CheckedFunction;
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
            .peek(either -> {if (either.isLeft()) System.out.println(either.getLeft());})
            .filter(Either::isRight)
            .map(Either::getRight)
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
            .filter(rec -> !rec.failed())
            .map(Result::result)
            .toList();
        
        check(res);
    }

    @Test(expected = NumberFormatException.class)
    public void intList() throws NumberFormatException {
        var list = Stream.of(NUMS)
        .map(toOptional(Integer::parseInt)) //Runtime NumberFormatException may happen here
        .flatMap(Optional::stream)
        .toList();
    }

    @Test(expected = NumberFormatException.class)
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
        if (r.failed()) {
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
    
    
    void foo() {
        Function<String,CompletableFuture<URL>> futureFunc = TryUtils.toFuture(URL::new);
        CompletableFuture<URL> url = futureFunc.apply("bar");
        
        url.thenCompose(TryUtils.toFuture(URL::openConnection))
        .thenCompose(TryUtils.toFuture(URLConnection::getOutputStream));
        

        
        CompletableFuture<Integer> f = CompletableFuture.completedFuture(1);
        f.thenApply(i -> "");
        
        
        
        
    }

//    @Test
//    public void testSocket() throws Exception {
//        Optional.of("host")
//        .flatMap(TryUtils.toOptional(h -> new Socket(h, 8848)))
//        .flatMap(TryUtils.toOptional(Socket::getOutputStream))
//        .ifPresent(TryUtils.toOptional(out -> out.write(new byte[] {1,2,3})));
//    }
}
