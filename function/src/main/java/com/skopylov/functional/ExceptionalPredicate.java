package com.skopylov.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Predicate;

/**
 * Predicate that may throw an Exception.
 * 
 * @author skopylov@gmail.com
 *
 * @param <T> type
 */

@FunctionalInterface
public interface ExceptionalPredicate<T> extends Predicate<T>{
    
    default boolean test(T t) {
        try {
            return testWithException(t);
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        } catch (Exception e) {
            throw new TryException(e);
        }
    }
    
    boolean testWithException(T t) throws Exception;

}
