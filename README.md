# java-functional-addons

![example workflow](https://github.com/skopylov58/java-functional-addons/actions/workflows/gradle.yml/badge.svg)

[JavaDocs](https://skopylov58.github.io/java-functional-addons/)

## `Try<T>` - functional exception handling

### Rational behind `Try<T>`

I really like Java's functional features (streams, optionals, etc.) very much but it becomes painful 
when using them in the real context. That is because Java's functional interfaces are Exception unaware
and you must handle checked exceptions inside lambdas.
Lets look at simple procedure of converting list of strings to list of URLs.

```java
    private List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls)
        .map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException me) { // Ops..., not too pretty
                return null;
            }
        }).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
```

`Try<T>` presents computation that may produce success result of type T or failure with exception.
It is quite similar to Java `Optional<T>` which may have result value of type T or nothing (null value).
With `Try<T>`, sample above will look like this:

```java
    private List<URL> urlListWithTry(String[] urls) {
        return Stream.of(urls)
            .map(Try.of(URL::new))
            .flatMap(Try::stream) //Failure gives empty stream
            .collect(Collectors.toList());
    }
```
### `Try<T>` with `Stream<T>` and `Optional<T>`

`Try<T>` can be easily converted  to the `Optional<T>` by using `Try#optional()` such way that failed Try will be converted to the `Optional.empty`.

I have intentionally made `Try<T>` API very similar to the Java `Optional<T>` API.
`Try#filter(Predicate<T>)` and`Try#map(Function<T,R>)` methods have the same semantics as corresponding Optional methods.
So if you are familiar to `Optional<T>` then you will get used to `Try<T>` with easy.

`Try<T>` can be easily converted  to the `Stream<T>` by using `Try#stream()` the same way as it is done for `Optional#stream()`.
Successful `Try<T>` will be converted to one element stream of type T, failed Try will be converted to the empty stream.

You can filter failed tries in the stream two possible ways - first is traditional by using `Try#filter()`

```
    ...
    .filter(Try::isSuccess)
    .map(Try::get)
    ...

```

Second approach is a bit shorter by using `Try#stream()`

```
    ...
    .flatMap(Try::stream)
    ...
```

This code will filter failed tries and return stream of successful values of type T.

### Recovering failed `Try<T>`

Do you have plans to recover from failures? If yes then `Try<T>` will help you to recover with easy.
You can chain as many recover strategies as you want.

```java
Try.of(...)
  .map(...)
  .map(...)
  .filter(...)
  .recover(recoverPlanA)
  .recover(recoverPlanB)
  .recover(recoverPlanC)
  .onSuccess(...)
  .orElse(...)
```

If planA succeeds then PlanB and PlanC will not have effect (will not be invoked). Explanation is 
simple - recover procedure has no effect for successful Try, so the only the first successful
plan will be in action.

### `Try<T>` with resources

`Try<T>` implements AutoCloseable interface, and `Try<T>` can be used inside `try-with-resource` block. 
Let's imagine we need open socket, write a few bytes to the socket's output stream and then close socket.
This code with `Try<T>` will look as following:

```
    try (var s = Try.of(() -> new Socket("host", 8888))) {
        s.map(Socket::getOutputStream)
        .onSuccess(out -> out.write(new byte[] {1,2,3}))
        .onFailure(e -> System.out.println(e));
    }
```

Socket will be closed after last curly bracket.

[See also articles on habr.com](https://habr.com/ru/post/676852/)

[One more](https://habr.com/ru/post/687954/)

