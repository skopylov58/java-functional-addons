package com.skopylov.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Consumer that may throw an Exception.
 * 
 * @author sergey.kopylov@hpe.com
 *
 * @param <T>
 */
@FunctionalInterface
public interface ExceptionalConsumer<T> extends Consumer<T>{
    
    default void accept(T t) {
        try {
            acceptWithException(t);
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        } catch (Exception e) {
            throw new TryException(e);
        }
    }
    
    void acceptWithException(T t) throws Exception;
    
}
