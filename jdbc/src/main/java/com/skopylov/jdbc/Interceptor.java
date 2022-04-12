package com.skopylov.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Duration;

public interface Interceptor {
    Object[] beforeCall(Object target, Method method, Object[] args) throws Exception;
    void afterCall(Object result, Throwable throwable, Duration dur, long threadId, String threadName) throws Exception;
    void newConnection(Connection c, long threadNum, String threadName) throws Exception;
}
