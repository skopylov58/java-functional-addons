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

See `Try<T>` javadoc API [here](try/javadoc/index.html)

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

## Simple JDBC connection pool

  

