package com.github.skopylov58.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.Duration;

/**
 * Interface for intercepting JDBC calls.
 * 
 * @author skopylov@gmail.com
 *
 */
public interface Interceptor {
    
    /**
     * This method will be invoked before JDBC call.
     * <p>
     * You can modify method arguments by returning new arguments.
     * 
     * @param target object on which call is performed
     * @param method method
     * @param args method arguments
     * @return new arguments for the call or null
     * @throws Exception
     */
    Object[] beforeCall(Object target, Method method, Object[] args) throws Exception;
    
    /**
     * This method will be invoked after successful JDBC call.
     * @param result JDBC result
     * @param dur JDBC call duration
     * @param threadId thread Id
     * @param threadName thread name
     * @throws Exception
     */
    void onSuccess(Object result, Duration dur, long threadId, String threadName) throws Exception;
    
    /**
     * This method will be invoked after failed JDBC call.
     * @param th cause exception 
     * @param dur JDBC call duration
     * @param threadId thread Id
     * @param threadName thread name
     * @throws Exception
     */
    void onFailure(Throwable th, Duration dur, long threadId, String threadName) throws Exception;
    
    /**
     * This method will be invoked after obtaining new JDBC connection through MiddleManJDBCDriver.
     * <p>
     * Here you can potentially modify connection properties like auto commit, 
     * transaction isolation level, network timeout, etc.
     * 
     * @param connection JDBC connection
     * @param threadId thread Id
     * @param threadName thread name
     * @throws Exception
     */
    void newConnection(Connection connection, long threadId, String threadName) throws Exception;
}
