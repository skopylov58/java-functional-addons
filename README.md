# java-async-retry

## `Try<T>` - functional exception handling

`Try<T>` presents computation that may produce success result of type T or failure with exception. It is quite similar to Java `Optional<T>` which may have result value of type T or nothing (null pointer problem). 

I've intentionally made `Try<T>` API a bit similar to `Optional<T>` to smooth learning/using curve. If you familiar to Optional then you are ready to use Try with easy.


## `Retry<T>` - asynchronous retry procedure

Retry is ancient strategy of failure recovering. We using Retry a lot when connecting to databases, sending e-mails, etc., etc.

Single Java class `Retry<T>` utility to perform asynchronous retry procedure on given `Supplier<T>` or Runnable using only CompletableFutures.

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

  

