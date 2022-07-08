package com.github.skopylov58.jdbc;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.skopylov58.jdbc.Interceptor;
import com.github.skopylov58.jdbc.MiddleManJDBCDriver;
import com.github.skopylov58.jdbc.SimpleLoggingInterceptor;

public class InterceptorTest {

    @Test
    public void testLoadInterceptor() throws Exception {
        Interceptor loadInterceptor = MiddleManJDBCDriver.loadInterceptor(SimpleLoggingInterceptor.class.getName());
        assertNotNull(loadInterceptor);
    }
    
    @Test
    public void testError() throws Exception {
        MiddleManJDBCDriver.loadInterceptor(null);
        MiddleManJDBCDriver.loadInterceptor("java.util.Date");
        MiddleManJDBCDriver.loadInterceptor("foo.bar");
    }
}
