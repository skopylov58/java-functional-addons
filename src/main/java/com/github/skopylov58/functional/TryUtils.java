package com.github.skopylov58.functional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.skopylov58.functional.Try.CheckedConsumer;

/**
 * Collection of higher-order functions to handle exceptions in functional style.
 * 
 * @author skopylov@gmail.com
 *
 */
public interface TryUtils {

    /**
     * Function that may throw an exception.
     */
    @FunctionalInterface
    interface CheckedFunction<T, R > {
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
        public ResultJava8(T t) {value = t;}
        public ResultJava8(Exception e ) {value = e;}
        public boolean failed () {return value instanceof Exception;} 
        public T result() {return (T) value;}
        public Exception cause() {return (Exception) value;}
    }

    /** Try Result for Java 14+ */
    @SuppressWarnings("unchecked")
    public record Result<T>(T result, Exception exception) {
        public static <T> Result<T> success(T t) {return new Result<>(t, null);}
        public static <T> Result<T> failure(Exception e) {return new Result<>(null, e);}
        public boolean isFailure() {return exception != null;}
        public boolean isSuccess() {return !isFailure();}
        public <R> Result<R> map(Function<T,R> mapper) {return isSuccess() ? success(mapper.apply(result)) : (Result<R>) this;}
        public <R> Result<R> flatMap(Function<T,Result<R>> mapper) {return isSuccess() ? mapper.apply(result) : (Result<R>) this;}
        public Result<T> filter(Predicate<T> pred) {return isSuccess() ? pred.test(result) ? this : failure(new NoSuchElementException()) : this;}
        public Stream<T> stream() {return isFailure() ? Stream.empty() : Stream.of(result);}
        public Optional<T> optional() {return isFailure() ? Optional.empty() : Optional.ofNullable(result);}
        public <R> R fold(Function<T,R> onSuccess, Function<Exception, R> onFailure) {return isSuccess() ? onSuccess.apply(result) : onFailure.apply(exception);}
        public Result<T> fold(Consumer<T> onSuccess, Consumer<Exception> onFailure) {if(isSuccess()) onSuccess.accept(result); else onFailure.accept(exception); return this;}
        public <R> Result<R> handle(BiFunction<T, Exception, Result<R>> handler) {return handler.apply(result, exception);}
        public Result<T> recover(Supplier<Result<T>> supplier) {return isFailure() ? supplier.get() : this;}
        public Result<T> recover(Function<Exception, Result<T>> errorMapper) {return isFailure() ? errorMapper.apply(exception) : this;}
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
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Result<R>}
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
    
//    default void foo() {
//        var r = Result.success(1);
//        r.fold(System.out::println, e -> {});
//        r.handle((i,e) -> null);
//        
//        var s = Result.catching(() -> new Socket("host", 1234))
//                .flatMap(Result.catching(Socket::getOutputStream))
//                .flatMap(Result.catching(o -> {
//                    o.write("Hello, world".getBytes());
//                    return o;
//                }))
//                .flatMap(Result.catching(o -> {o.close(); return o;}))
//                ;
//    }
    
    
    /**
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Optional<R>}
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function {@code T=>R} that may throw checked exception
     * @return total function {@code T=>Optional<R>}
     */
    static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func) {
    	return toOptional(func, (t, e) -> {});
    }

    /**
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Optional<R>}
     * Gives access to Exception and corresponding input value.
     * 
     * @param <T> function input parameter type
     * @param <R> function result type
     * @param func partial function {@code T=>R} that may throw checked exception
     * @param consumer has access to exception and corresponding input value
     * @return total function {@code T=>Optional<R>}
     */
    static <T, R> Function<T, Optional<R>> toOptional(CheckedFunction<T, R> func, BiConsumer<T, Exception> consumer) {
        return param -> {
            try {
                return Optional.ofNullable(func.apply(param));
            } catch (Exception e) {
            	consumer.accept(param, e);
                return Optional.empty();
            }
        };
    }

    static <T, R> Function<T, R> catchingMapper(CheckedFunction<T, R> func, BiFunction<T, Exception, R> errorMapper) {
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
     * Higher-order function to convert partial supplier {@code ()=>T} to total supplier {@code ()=>Optional<T>}
     * @param <T> supplier result type
     * @param supplier {@code ()=>T} that may throw an exception
     * @return total supplier {@code ()=>Optional<T>}
     */
    static <T> Supplier<Optional<T>> toOptional(CheckedSupplier<T> supplier) {
        return toOptional(supplier, e -> {});
    }

    /**
     * Higher-order function to convert partial supplier {@code ()=>T} to total supplier {@code ()=>Optional<T>}
     * @param <T> supplier result type
     * @param supplier {@code ()=>T} that may throw an exception
     * @param consumer error consumer
     * @return total supplier {@code ()=>Optional<T>}
     */
    static <T> Supplier<Optional<T>> toOptional(CheckedSupplier<T> supplier, Consumer<Exception> consumer) {
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
     * Higher-order function to convert partial function {@code T=>R} to total function {@code T=>Either<R, Exception>}
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
                return null;  // we never will get here
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
    
    static <T, R> Function<T, R> memoize(Function<T, R> func, Map<T, R> cache) {
    	return t -> cache.computeIfAbsent(t, func::apply);
    }

    static <T, R> Function<T, R> memoize(Function<T, R> func) {
    	return memoize(func, new ConcurrentHashMap<>());
    }
    
    class BooleanLatch {
    	boolean opened = true;
    	boolean get() {
    		boolean retVal = opened;
   			opened = false;
    		return retVal;
    	}
    }

    private static Runnable once(Runnable run, BooleanLatch latch) {
    	return () -> {
    		if (latch.get()) {
    			run.run();
    		}
    	};
    }
    
    static Runnable once(Runnable run) {
    	return once(run, new BooleanLatch());
    }
    
    /**
     * Consumer to UnaryOperator conversion.
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

    
}
