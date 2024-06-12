package com.github.skopylov58.functional;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import com.github.skopylov58.functional.Try.CheckedConsumer;

/**
 * Collection of useful higher-order functions to handle exceptions in functional style.
 * 
 * @author skopylov@gmail.com
 *
 */
public interface FPUtils {

  /**
   * Function that may throw an exception.
   */
  @FunctionalInterface
  interface CheckedFunction<T, R> {
    R apply(T t) throws Exception;
  }

  /** Supplier that may throw an exception. */
  @FunctionalInterface
  interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  /** Try Result for Java 8+ */
  public static class ResultJava8<T> {
    private final Object value;

    public ResultJava8(T t) {
      value = t;
    }

    public ResultJava8(Exception e) {
      value = e;
    }

    public boolean failed() {
      return value instanceof Exception;
    }

    public T result() {
      if (failed()) {
        throw new NoSuchElementException("Cause: " + value);
      }
      return (T) value;
    }

    public Exception cause() {
      if (!failed()) {
        throw new IllegalStateException("Value: " + value);
      }
      return (Exception) value;
    }
  }

  /** Try Result for Java 14+ */
  @SuppressWarnings("unchecked")
  public record Result<T>(T result, Exception exception) {
    public static <T> Result<T> success(T t) {
      return new Result<>(t, null);
    }

    public static <T> Result<T> failure(Exception e) {
      return new Result<>(null, e);
    }

    public boolean isFailure() {
      return exception != null;
    }

    public boolean isSuccess() {
      return !isFailure();
    }

    public <R> Result<R> map(Function<T, R> mapper) {
      return isSuccess() ? success(mapper.apply(result)) : (Result<R>) this;
    }

    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
      return isSuccess() ? mapper.apply(result) : (Result<R>) this;
    }

    public Result<T> filter(Predicate<T> pred) {
      return isSuccess() ? pred.test(result) ? this : failure(new NoSuchElementException()) : this;
    }

    public Stream<T> stream() {
      return isFailure() ? Stream.empty() : Stream.of(result);
    }

    public Optional<T> optional() {
      return isFailure() ? Optional.empty() : Optional.ofNullable(result);
    }

    public <R> R fold(Function<T, R> onSuccess, Function<Exception, R> onFailure) {
      return isSuccess() ? onSuccess.apply(result) : onFailure.apply(exception);
    }

    public Result<T> fold(Consumer<T> onSuccess, Consumer<Exception> onFailure) {
      if (isSuccess())
        onSuccess.accept(result);
      else
        onFailure.accept(exception);
      return this;
    }

    public <R> Result<R> handle(BiFunction<T, Exception, Result<R>> handler) {
      return handler.apply(result, exception);
    }

    public Result<T> recover(Supplier<Result<T>> supplier) {
      return isFailure() ? supplier.get() : this;
    }

    public Result<T> recover(Function<Exception, Result<T>> errorMapper) {
      return isFailure() ? errorMapper.apply(exception) : this;
    }

    static <T, R> Function<T, Result<R>> catching(CheckedFunction<T, R> func) {
      return param -> {
        try {
          return success(func.apply(param));
        } catch (Exception e) {
          return failure(e);
        }
      };
    }

    static <T> Result<T> catching(CheckedSupplier<T> supplier) {
      try {
        return success(supplier.get());
      } catch (Exception e) {
        return failure(e);
      }
    }
  }

  /**
   * Higher-order function to convert partial function {@code T=>R} to total function
   * {@code T=>Result<R>}
   * 
   * @param <T> function input parameter type
   * @param <R> function result type
   * @param func partial function {@code T=>R} that may throw checked exception
   * @return total function {@code T=>Result<R>}
   */
  static <T, R> Function<T, Result<R>> toResult(CheckedFunction<T, R> func) {
    return param -> {
      try {
        return Result.success(func.apply(param));
      } catch (Exception e) {
        return Result.failure(e);
      }
    };
  }

  default <T> Result<T> onSuccessCatching(Result<T> result, CheckedConsumer<T> cons) {
    CheckedFunction<T, T> f = param -> {
      cons.accept(param);
      return param;
    };
    return result.flatMap(Result.catching(f));
  }

  /**
   * Higher-order function to convert partial function {@code T=>R} to total function
   * {@code T=>Optional<R>}
   * 
   * @param <T> function input parameter type
   * @param <R> function result type
   * @param func partial function {@code T=>R} that may throw checked exception
   * @return total function {@code T=>Optional<R>}
   */
  static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func) {
    return toOptional(func, (t, e) -> {
    });
  }

  /**
   * Higher-order function to convert partial function {@code T=>R} to total function
   * {@code T=>Optional<R>} Gives access to Exception and corresponding input value.
   * 
   * @param <T> function input parameter type
   * @param <R> function result type
   * @param func partial function {@code T=>R} that may throw checked exception
   * @param consumer has access to exception and corresponding input value
   * @return total function {@code T=>Optional<R>}
   */
  static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func,
      BiConsumer<T, Exception> consumer) {
    return param -> {
      try {
        return Optional.ofNullable(func.apply(param));
      } catch (Exception e) {
        consumer.accept(param, e);
        return Optional.empty();
      }
    };
  }

  static <T, R> Function<T, R> catchingMapper(CheckedFunction<T, R> func,
      BiFunction<T, Exception, R> errorMapper) {
    return param -> {
      try {
        return func.apply(param);
      } catch (Exception e) {
        return errorMapper.apply(param, e);
      }
    };
  }

  static <T, R> Function<T, R> throwingMapper(CheckedFunction<T, R> func) {
    return param -> {
      try {
        return func.apply(param);
      } catch (Exception e) {
        sneakyThrow(e);
        return null;
      }
    };
  }

  static <T, R> Function<T, R> catchingMapper(CheckedFunction<T, R> func, Supplier<R> supplier) {
    return catchingMapper(func, (t, e) -> supplier.get());
  }


  /**
   * Higher-order function to convert partial supplier {@code ()=>T} to total supplier
   * {@code ()=>Optional<T>}
   * 
   * @param <T> supplier result type
   * @param supplier {@code ()=>T} that may throw an exception
   * @return total supplier {@code ()=>Optional<T>}
   */
  static <T> Supplier<Optional<T>> toOptional(CheckedSupplier<T> supplier) {
    return toOptional(supplier, e -> {
    });
  }

  /**
   * Higher-order function to convert partial supplier {@code ()=>T} to total supplier
   * {@code ()=>Optional<T>}
   * 
   * @param <T> supplier result type
   * @param supplier {@code ()=>T} that may throw an exception
   * @param consumer error consumer
   * @return total supplier {@code ()=>Optional<T>}
   */
  static <T> Supplier<Optional<T>> toOptional(CheckedSupplier<T> supplier,
      Consumer<Exception> consumer) {
    return () -> {
      try {
        return Optional.ofNullable(supplier.get());
      } catch (Exception e) {
        consumer.accept(e);
        return Optional.empty();
      }
    };
  }

  /**
   * Higher-order function to convert partial function {@code T=>R} to total function
   * {@code T=>Either<R, Exception>}
   * 
   * @param <T> function input parameter type
   * @param <R> function result type
   * @param func partial function {@code T=>R} that may throw an exception
   * @return total function {@code T=>Either<R, Exception>}
   */
  static <T, R> Function<T, Either<Exception, R>> toEither(CheckedFunction<T, R> func) {
    return param -> {
      try {
        return Either.right(func.apply(param));
      } catch (Exception e) {
        return Either.left(e);
      }
    };
  }

  static <T, R> Function<T, ResultJava8<R>> toResultJava8(CheckedFunction<T, R> func) {
    return param -> {
      try {
        return new ResultJava8<>(func.apply(param));
      } catch (Exception e) {
        return new ResultJava8<>(e);
      }
    };
  }

  static <T, R> Function<T, CompletableFuture<R>> toFuture(CheckedFunction<T, R> func) {
    return param -> {
      try {
        return CompletableFuture.completedFuture(func.apply(param));
      } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  static <T> Supplier<T> toSupplier(CheckedSupplier<T> supplier) {
    return () -> {
      try {
        return supplier.get();
      } catch (Exception e) {
        sneakyThrow(e);
        return null; // we never will get here
      }
    };
  }

  static Duration measure(Runnable runnable) {
    Instant start = Instant.now();
    runnable.run();
    Instant end = Instant.now();
    return Duration.between(start, end);
  }

  static <T, R> Function<T, R> after(Function<T, R> func, Consumer<R> after) {
    return t -> {
      R r = func.apply(t);
      after.accept(r);
      return r;
    };
  }

  static <T, R> Function<T, R> before(Function<T, R> func, Consumer<T> before) {
    return t -> {
      before.accept(t);
      return func.apply(t);
    };
  }

  public static <T, R> Function<T, R> memoize(Function<T, R> func, Map<T, R> cache) {
    return t -> cache.computeIfAbsent(t, func::apply);
  }

  public static <T, R> Function<T, R> memoize(Function<T, R> func) {
    return memoize(func, new ConcurrentHashMap<>());
  }

  public static Runnable once(Runnable runnable, AtomicBoolean runFlag) {
    return () -> {
      if (runFlag.compareAndSet(false, true)) {
        runnable.run();
      }
    };
  }

  /**
   * Runs runnable exactly once.
   * @param run runnable to decorate
   * @return runnable that will run only once.
   */
  public static Runnable once(Runnable run) {
    return once(run, new AtomicBoolean(false));
  }
  
  public static <T> Supplier<T> memoize(Supplier<T> supplier, AtomicReference<T> ref) {
    return () -> ref.getAndUpdate(t -> t == null ? supplier.get() : t);
  }

  public static <T> Supplier<T> memoize(Supplier<T> supplier) {
    return memoize(supplier, new AtomicReference<T>());
  }
  
  /**
   * Consumer to UnaryOperator conversion.
   * 
   * @param <T> type T
   * @param cons consumer
   * @return UnaryOperator
   */
  static <T> UnaryOperator<T> ctou(Consumer<T> cons) {
    return t -> {
      cons.accept(t);
      return t;
    };
  }
  
  @FunctionalInterface
  interface Backoff extends Function<Long, Duration>{
    
    default Backoff withJitter(Supplier<Duration> jitter) {
      return i -> apply(i).plus(jitter.get());
    }
  }
  
  /**
   * Simple jitter using system timer.
   * @param duration maximum jitter value
   * @return random jitter between 0 and duration
   */
  static Supplier<Duration> simpleJitter(Duration duration) {
    long m = System.currentTimeMillis() % duration.toMillis();
    return () -> Duration.ofMillis(m);  
  }
  
  /**
   * Backoff strategy that starts with min value, increasing delay twice on each step, with max upper threshold.
   * @param min min delay in milliseconds
   * @param max max delay in milliseconds
   * @return exponential backoff
   */
  static Backoff exponentialBackoff(long min, long max) {
    if (min <= 0 || min >= max) {
      throw new IllegalArgumentException();
    }
    var maxDur = Duration.ofMillis(max);
    return i -> {
      if (i > 31) {
        return maxDur;
      }
      Duration dur = Duration.ofMillis(min << i);
      return dur.toMillis() > max ? maxDur : dur; 
    };
  }
  
 /**
  * Fixed delay backoff strategy
  * @param delay delay between tries
  * @return fixed backoff
  */
  static Backoff fixedDelay(Duration delay) {
    return i -> delay;
  }
  
  /**
   * Retry with fixed delay.
   * @param <V> resulting type
   * @param callable callable to retry
   * @param numOfRetries number of retries
   * @param delay delay between tries
   * @return Optional result
   */
  static <V> Optional<V> retry(Callable<V> callable, long numOfRetries, Duration delay) {
    return retry(callable, numOfRetries, fixedDelay(delay), x -> true);
  }

  /**
   * Retry with backoff strategy.
   * @param <V> resulting type
   * @param callable callable to retry
   * @param numOfRetries number of retries
   * @param backoff backoff strategy
   * @return Optional result
   */
  static <V> Optional<V> retry(Callable<V> callable, long numOfRetries, Backoff backoff) {
    return retry(callable, numOfRetries, backoff, x -> true);
  }

  static <V> Optional<V> retry(Callable<V> callable, long numOfRetries, Backoff backoff, Predicate<V> isSuccess) {
    for (long i = 0; i < numOfRetries; i++) {
      if (Thread.currentThread().isInterrupted()) {
        break;  //behave gracefully on interruption
      }
      try {
        V value = callable.call();
        if (isSuccess.test(value)) {
          return Optional.ofNullable(value);
        }
      } catch (Exception e) {
        System.getLogger(FPUtils.class.getName()).log(Level.INFO, e);
      }
      if (i == numOfRetries - 1) {
        // Last attempt was made, no need to sleep for next try
        break;
      }
      try {
        Thread.sleep(backoff.apply(i).toMillis());
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    return Optional.empty();
  }

  static <V> Optional<V> retry2(Callable<V> callable, 
      LongPredicate lp,
      Backoff backoff,
      Predicate<V> isSuccess) 
  {
    return  LongStream.iterate(0, i -> i + 1)
    .takeWhile(lp::test)
    .takeWhile(i -> i == 0 || sleep(backoff.apply(i)))
    .mapToObj(i -> call(callable))
    .map(opt -> opt.filter(isSuccess))
    .flatMap(Optional::stream)
    .findFirst();
  }

  static boolean sleep(Duration delay) {
    if (Thread.currentThread().isInterrupted()) {
      return false;
    }
    try {
      Thread.sleep(delay.toMillis());
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
  
  static <T> Optional<T> call(Callable<T> callable) {
    try {
      return Optional.ofNullable(callable.call());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

}
