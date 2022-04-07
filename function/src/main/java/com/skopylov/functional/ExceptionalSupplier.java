package com.skopylov.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;


/**
 * Supplier that may throw an Exception.
 * 
 * @author skopylov@gmail.com
 *
 * @param <T> type returned by supplier.
 */
@FunctionalInterface
public interface ExceptionalSupplier<T> extends Supplier<T> {
    
    @Override
    default T get() {
        try {
            return getWithException();
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } catch (Exception e) {
            throw new TryException(e);
        }
    }
    
    T getWithException() throws Exception;

    static <T> Supplier<T> uncheck(ExceptionalSupplier<T> s) {
        return s::get;
    }
}

