package com.github.skopylov58.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Duration;
import java.util.Arrays;

public class SimpleLoggingInterceptor implements Interceptor {

    @Override
    public Object[] beforeCall(Object target, Method method, Object[] args) throws Exception {
        String msg = String.format("Invoke: %s with params %s", method.getName(), Arrays.toString(args));
        System.out.println(msg);
        return null;
    }

    @Override
    public void onSuccess(Object result, Duration dur, long threadId, String threadName)
            throws Exception {
        String msg = String.format("Result: %s took %s", result, dur);
        System.out.println(msg);
        
    }

    @Override
    public void onFailure(Throwable th, Duration dur, long threadId, String threadName) throws Exception {
        String msg = String.format("Failure: %s took %s", th.getMessage(), dur);
        System.out.println(msg);
        th.printStackTrace();
    }
    
    @Override
    public void newConnection(Connection c, long threadNum, String threadName) throws Exception {
        System.out.println("New connection " + c);
    }

}
