package com.skopylov.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import com.skopylov.functional.Try;

public class MiddleManJDBCDriver implements Driver {
    
    public static final String JDBC_MIDDLEMAN = "jdbc:middleman:";
    static Interceptor interceptor;

    static {
        Try.of(() -> DriverManager.registerDriver(new MiddleManJDBCDriver())).orElseThrow();
        interceptor = loadInterceptor(System.getProperty("jdbc.interceptor"));
    }

    static Interceptor loadInterceptor(String className) {
        return Try.of(() -> className)
        .filter(Objects::nonNull)
        .map(s -> Thread.currentThread().getContextClassLoader().loadClass(s))
        .map(c -> (Interceptor)c.getConstructor().newInstance())
//        .filter(Interceptor.class::isInstance, 
//                c -> System.out.println(c.getClass().getName() + " should be Interceptor"))
        //.cast(Interceptor.class)
        .optional()
        .orElse(new SimpleLoggingInterceptor());
    }
    
    @Override
    public Connection connect(String url, Properties info) throws SQLException {

        if (!acceptsURL(url)) {
            return null;
        }
        
        Connection connection = DriverManager.getConnection(url.replace(JDBC_MIDDLEMAN, ""), info);

        Thread curThread = Thread.currentThread();
        Try.of(() -> interceptor.newConnection(connection, curThread.getId(), curThread.getName()));
        
        Object connProxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] { Connection.class },
                new JDBCProxy(connection));

        return (Connection) connProxy;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(JDBC_MIDDLEMAN);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo [] {};
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
    
}