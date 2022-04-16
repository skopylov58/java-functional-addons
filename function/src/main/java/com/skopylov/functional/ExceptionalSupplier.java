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
    
    /**
     * Gets result
     * @return result of type T
     * @throws Exception
     */
    T getWithException() throws Exception;
    
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

    static <T> Supplier<T> uncheck(ExceptionalSupplier<T> s) {
        return s::get;
    }

    /**
     * Converts Supplier to ExceptionalSupplier
     * @param <T> type
     * @param s Supplier<T> supplier
     * @return ExceptionalSupplier 
     */
    static <T> ExceptionalSupplier<T> checked(Supplier<T> s) {
        return s::get;
    }

}

