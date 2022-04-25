# java-async-retry

![example workflow](https://github.com/skopylov58/java-functional-addons/actions/workflows/gradle.yml/badge.svg)

## `Try<T>` - functional exception handling

### Rational behind `Try<T>`

I really like Java's functional features (streams, optionals, etc.) very much but it becomes painful 
when using them in the real context. That is because Java's functional interfaces are Exception unaware
and you must handle checked exceptions inside lambdas.
Lets look at simple procedure of converting list of strings to list of URLs.

```java
    private List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls).map(s -> {
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
It is quite similar to Java `Optional<T>` which may have result value of type T or nothing (null pointer problem).
From the technical point of view, `Try<T>` is specialization of `Either<T, Exception>` interface.
With `Try<T>`, sample above will look like this:

```java
    private List<URL> urlListWithTry(String[] urls) {
        return Stream.of(urls).map(s -> Try.of(() -> new URL(s)))
             .flatMap(Try::stream) //Failure gives empty stream
             .collect(Collectors.toList());
    }
```
### `Try<T>` with `Stream<T>` and `Optional<T>`

`Try<T>` can be seamlessly converted to `Stream<T>` or `Optional<T>`.
I've intentionally made `Try<T>` API similar in some way to `Stream<T>` and `Optional<T>` 
interfaces to smooth learning/using curve. If you familiar to Stream/Optional then you are ready to use Try with easy.

```java
Try.of(...)
  .map(...)
  .map(...)
  .filter(...)
  .recover(...)
  .recover(...)
  .onSuccess(...)
  .orElse(...)
```

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

```java
Try.of(() -> new FileInputStream("path/to/file")) // !!! hey, how we are going close this input stream?
  .map(...)
  .getOrElse(...)
```

`Try<T>` `autoClose()` method marks auto-closeable resources to be closed in some appropriate moment.
So the code above should be re-written as follows:

```java
Try.of(() -> new FileInputStream("path/to/file"))
  .autoClose() // marks input stream to close in the future
  .map(...)
  ...
  .closeResources() //closes all marked resources
  .getOrElse(...)
```

`Try<T>` implements AutoCloseable interface and all marked resources will be closed automatically if Try is used inside Java' `try-with-resource` block.

## `Retry<T>` - asynchronous retry procedure

Retry is ancient strategy of failure recovering. We using Retry a lot when connecting to databases, sending e-mails, etc., etc.

Single Java class `Retry<T>` utility to perform asynchronous retry procedure on given `Supplier<T>` or `Runnable` using only CompletableFutures.

Sample usage:

```java
CompletableFuture<Connection> futureConnection = 
Retry.of(() -> DriverManager.getConnection("jdbc:mysql:a:b"))
  .maxTries(100)
  .delay(1, TimeUnit.SECONDS)
  .withErrorHandler(...)
  .withExecutor(...)
  .retry();
```

## JDBC proxy driver (JDBCMiddleman)

### What for?

JDBCMiddleman driver can be used for:

- performance benchmarking of your JDBC code
- looking what really happens under the hood
- troubleshooting your code
- simulating network latencies
- discovering uncaught exceptions
- intercepting any JDBC call with your custom interceptors

### How to use

1. Put this driver into your's application class-path.
2. Prepend your's DB URL with "jdbc:middleman:" prefix, for example "jdbc:middleman:jdbc:mysql://localhost:3306/foo"
3. Done
4. Run your application and observe info in the standard output.

### Custom interceptors

JDBCMiddleman driver is shipped with default `SimpleLoggingInterceptor` which logs to the standard output JDBC invocations and corresponding results with nanoseconds precision:

```
Invoke: prepareStatement with params [INSERT INTO NAMES (name, age) VALUES (?, ?)]
Result prep0: INSERT INTO NAMES (name, age) VALUES (?, ?) took PT0.0019989S
Invoke: setString with params [1, John]
Result null took PT0S
Invoke: setInt with params [2, 40]
Result null took PT0.0010007S
Invoke: executeUpdate with params null
Result 1 took PT0.0019985S
Invoke: executeQuery with params [SELECT * from NAMES]
Result rs3: org.h2.result.LocalResult@543588e6 columns: 2 rows: 1 pos: -1 took PT0.0130346S
Invoke: next with params null
Result true took PT0S
Invoke: getString with params [1]
Result John took PT0S
Invoke: getInt with params [2]
Result 40 took PT0S
John 40
```

You may create your own interceptor by implementing 
[com.skopylov.jdbc.Interceptor](jdbc/src/main/java/com/skopylov/jdbc/Interceptor.java) interface.
To bring your interceptor into action, please:

- put your's interceptor into application class-path 
- specify system property `jdbc.interceptor` with your's interceptor full class name


## Simple JDBC connection pool

  

