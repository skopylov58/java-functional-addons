package com.github.skopylov58.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class MiddleManJDBCDriver implements Driver {
    
    public static final String JDBC_MIDDLEMAN = "jdbc:middleman:";
    static Interceptor interceptor;

    static {
        try {
            DriverManager.registerDriver(new MiddleManJDBCDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Errorr loading MiddleManJDBCDriver", e);
        }
        interceptor = loadInterceptor(System.getProperty("jdbc.interceptor", SimpleLoggingInterceptor.class.getName()));
    }

    static Interceptor loadInterceptor(String className) {
        try {
            Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(className);
            return (Interceptor) c.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error loading jdbc.interceptor - " + className, e);
        }
    }
    
    @Override
    public Connection connect(String url, Properties info) throws SQLException {

        if (!acceptsURL(url)) {
            return null;
        }
        
        Connection connection = DriverManager.getConnection(url.replace(JDBC_MIDDLEMAN, ""), info);

        try {
            interceptor.newConnection(connection, Thread.currentThread().getId(), Thread.currentThread().getName());
        } catch (Exception e) {
            throw new SQLException("Error intercepting new connection", e);
        }
        
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