# java-async-retry

## `Try<T>` - functional exception handling

`Try<T>` presents computation that may produce success result of type T or failure with exception. It is quite similar to Java `Optional<T>` which may have result value of type T or nothing (null pointer problem). 

I've intentionally made `Try<T>` API similar in some way to `Optional<T>` to smooth learning/using curve. If you familiar to Optional then you are ready to use Try with easy.

```java
Try.of(...)
  .map(...)
  .map(...)
  .filter(...)
  .recover(...)
  .recover(...)
  .logException(...)
  .onSuccess(...)
  .getOrDefault(...)
```

`Try<T>` seamlessly integrated with Java's `Stream<T>` API.
Example shows converting array of strings to URLs.

```java
    private List<URL> urlListWithTry(String[] urls) {
        return Stream.of(urls).map(s -> Try.of(() -> new URL(s)))
                .peek(Try::logException)
                .flatMap(Try::stream) //Failure gives empty stream
                .collect(Collectors.toList());
    }
```

Without `Try<T>` you must handle `MalformedURLException` explicitly.

```java
    private List<URL> urlListTraditional(String[] urls) {
        return Stream.of(urls).map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException me) {
                TestBase.logException(me);
                return null;
            }
        }).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
```

`Try<T>` emulates Java's `try-with-resource` feature with `autoClose()` Try's method.

```java
Try.of(() -> new FileInputStream("path/to/file")).autoClose()
  .map(...)
  .getOrThrow() // <- this will automatically close FileInputStream
```

See `Try<T>` [javadoc API here](https://github.com/skopylov58/java-async-retry/blob/master/try/javadoc/)

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

  

