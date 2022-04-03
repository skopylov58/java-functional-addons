# java-async-retry
Single Java class Retry<T> utility to perform asynchronous retry procedure on given Supplier or Runnable using only CompletableFutures.

Sample usage
```java
CompletableFuture<Connection> futureConnection = 
Retry.of(() -> DriverManager.getConnection("jdbc:mysql:a:b"))
  .maxTries(100)
  .delay(1, TimeUnit.SECONDS)
  .withErrorHandler(...)
  .withExecutor(...)
  .retry();
```
