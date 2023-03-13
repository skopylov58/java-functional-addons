package com.github.skopylov58.jdbc;

import static org.junit.Assert.*;

import org.junit.Test;

public class InterceptorTest {

    @Test
    public void testLoadInterceptor() throws Exception {
        Interceptor loadInterceptor = MiddleManJDBCDriver.loadInterceptor(SimpleLoggingInterceptor.class.getName());
        assertNotNull(loadInterceptor);
    }
    
//    @Test
//    public void testError() throws Exception {
//        MiddleManJDBCDriver.loadInterceptor(null);
//        MiddleManJDBCDriver.loadInterceptor("java.util.Date");
//        MiddleManJDBCDriver.loadInterceptor("foo.bar");
//    }
}
