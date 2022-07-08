package com.github.skopylov58.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Consumer that may throw an Exception.
 * 
 * @author skopylov@gmail.com
 *
 * @param <T> type
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

    default Exception accept0(T t) {
        try {
            acceptWithException(t);
            return null;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return e;
        }
    }

    
    void acceptWithException(T t) throws Exception;
    
}
