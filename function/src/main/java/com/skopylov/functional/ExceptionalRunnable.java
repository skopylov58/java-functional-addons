package com.skopylov.functional;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Runnable that may throw an Exception.
 * 
 * @author skopylov@gmail.com
 *
 */

@FunctionalInterface
public interface ExceptionalRunnable extends Runnable{

    default void run() {
        try {
            runWithException();
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        } catch (Exception e) {
            throw new TryException(e);
        }
    }
    
    void runWithException() throws Exception;
    
    static Runnable uncheck(ExceptionalRunnable r) {
        return r::run;
    }

}
