package com.github.skopylov58.functional;
/**
 * Run-time exception which wraps checked exceptions.
 * 
 * @author sergey.kopylov@hpe
 *
 */

public class TryException extends RuntimeException {

    private static final long serialVersionUID = -5754178301667688301L;

    public TryException(Throwable th) {
        super(th);
    }
}
