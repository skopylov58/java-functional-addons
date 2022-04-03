# java-async-retry
Single Java class utility to perform asynchronous retry procedure on given Supplier using only CompletableFutures.

Retry<T> interface to perform asynchronous retry operations on supplier
and runnable that may throw an exception.

Retry.of(...) factory methods create RetryBuilder from Supplier or Runnable

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

@author Sergey Kopylov
@author skopylov@gmail.com
