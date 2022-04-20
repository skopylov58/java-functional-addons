package com.skopylov.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * Function that may throw an Exception.
 * @author skopylov@gmail.com
 *
 * @param <T> function parameter type
 * @param <R> function result type
 */

@FunctionalInterface
public interface ExceptionalFunction <T, R> extends Function<T, R>{
    
    default R apply(T t) {
        try {
            return applyWithException(t);
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        } catch (Exception e) {
            throw new TryException(e);
        }
    }
    
    R applyWithException(T t) throws Exception;
    
    static <T, R> Function<T,R> uncheck(ExceptionalFunction<T, R> func) {
        return func::apply;
    }

}
