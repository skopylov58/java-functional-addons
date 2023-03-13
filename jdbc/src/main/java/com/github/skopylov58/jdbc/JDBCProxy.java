package com.github.skopylov58.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

public class JDBCProxy implements InvocationHandler {

    private final Object target;

    public JDBCProxy(Object obj) {
        target = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Thread thr = Thread.currentThread();
        Instant start = null;
        Instant end = null;
        try {
            
            Object [] newArgs = MiddleManJDBCDriver.interceptor.beforeCall(target, method, args);
            if (newArgs == null) {
                newArgs = args;
            }

            //invoke
            start = Instant.now();
            Object result = method.invoke(target, newArgs);
            end = Instant.now();

            //after invoke
            Duration dur = Duration.between(start, end);
            MiddleManJDBCDriver.interceptor.onSuccess(result, dur, thr.getId(), thr.getName());
            
            if (result instanceof CallableStatement) {
                return makeProxy(CallableStatement.class, result);
            } else if (result instanceof PreparedStatement) {
                return makeProxy(PreparedStatement.class, result);
            } else if (result instanceof Statement) {
                return makeProxy(Statement.class, result);
            } else if (result instanceof ResultSet) {
                return makeProxy(ResultSet.class, result);
            } else {
                return result;
            }
        } catch (Throwable th) {
            end = Instant.now();
            Duration dur = Duration.between(start, end);
            MiddleManJDBCDriver.interceptor.onFailure(th, dur, thr.getId(), thr.getName());
            throw th;
        }
    }

    Object makeProxy(Class<?> clazz, Object result) {
        return Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] { clazz },
                new JDBCProxy(result));
    }
}
