package com.skopylov.jdbc.pool;

import java.lang.System.Logger.Level;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.skopylov.functional.ExceptionalSupplier;
import com.skopylov.functional.Try;
import com.skopylov.functional.Tuple;
import com.skopylov.retry.Retry;

public class JDBCConnectionPool {
    
    private static final String NO_AVAILABLE_CONNECTIONS = "No available connections in the pool";
    private int maxConnections = 10;
    
    private RetryOptions poolRetryOptions = new RetryOptions(10, 1, TimeUnit.SECONDS);
    private RetryOptions clientRetryOptions = new RetryOptions(10, 200, TimeUnit.MILLISECONDS);
    
    private boolean checkConnection = true;
    int connectionValidationTimeOut = 10; //in seconds

    private boolean checkOrphans = false;
    private Duration orphanDuration = Duration.ofSeconds(30);
    private ConcurrentHashMap<Connection, Tuple<Instant,StackTraceElement[]>> orphaned;
    private ScheduledExecutorService orpansWatchDog;

//    String userName;
//    String password;
    
    private final String dbUrl;

    private BlockingQueue<PooledConnection> pool = new LinkedBlockingQueue<>();
    
    public JDBCConnectionPool(String url) {
        dbUrl = url;
    }

    public JDBCConnectionPool(String url, int maxCon) {
        this(url);
        maxConnections = maxCon;
    }

    public void start() {
        for (int i = 0; i < maxConnections; i++) {
            aquireDbConnection(dbUrl);
        }
        if (checkOrphans) {
            orphaned = new ConcurrentHashMap<>(maxConnections);
            orpansWatchDog = Executors.newScheduledThreadPool(1);
            orpansWatchDog.schedule(this::checkOrphan, 1, TimeUnit.SECONDS);
        }
    }
    
    public void stop() {
        pool.forEach(c -> Try.of(c.getDelegate()::close));
        pool.clear();
        if (checkOrphans) {
            orpansWatchDog.shutdown();
        }
    }
    
    public Connection getConnection() throws SQLException {
        Connection result = Stream.generate(() -> 
            Try.of(this::getConnectionFromPool)
            .ifInterrupted()
            .optional())
        .limit(clientRetryOptions.numOfRetries) //Stream.generate().limit() emulates loop
        .flatMap(Optional::stream)              //Filter empty optional
        .findFirst()                            //stops on first connection
        .orElseThrow(() -> new SQLException(NO_AVAILABLE_CONNECTIONS));
        
        if (checkOrphans) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            orphaned.put(result, new Tuple<Instant, StackTraceElement[]>(Instant.now(),stackTrace));
        }
        return result;
    }

    public Connection getConnectionWithLoopAndTry() throws SQLException {
        for (long i = 0; i < clientRetryOptions.numOfRetries; i++) {
            Try<Connection> tried = Try.of(this::getConnectionFromPool)
                    .ifInterrupted()
                    .filter(Objects::nonNull);
            if (tried.isSuccess()) {
                return tried.optional().get();
            }
        }
        throw new SQLException(NO_AVAILABLE_CONNECTIONS);
    }

    public Connection getConnectionWithLoopAndOptional() throws SQLException {
        for (long i = 0; i < clientRetryOptions.numOfRetries; i++) {
            var opt = Try.of(this::getConnectionFromPool)
                    .ifInterrupted()
                    .optional();
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        throw new SQLException(NO_AVAILABLE_CONNECTIONS);
    }

    public Connection getConnectionTraditional() throws SQLException {
        for (long i = 0; i < clientRetryOptions.numOfRetries; i++) {
            try {
                Connection c = getConnectionFromPool();
                if (c != null) {
                    return c;
                }
            } catch (InterruptedException  ie) {
                Thread.currentThread().interrupt();
            } catch (SQLException e) {
                // continue
            }
        }
        throw new SQLException(NO_AVAILABLE_CONNECTIONS);
    }

    void checkOrphan() {
        orphaned.forEachValue(0, p -> {
            Duration d = Duration.between(p.first, Instant.now());
            if (d.compareTo(orphanDuration) > 0) {
                System.getLogger(JDBCConnectionPool.class.getName())
                .log(Level.WARNING, "Orphaned connection detected");
            }
        });
    }

    private Connection getConnectionFromPool() throws InterruptedException, SQLException {
        PooledConnection connection = pool.poll(clientRetryOptions.delay, clientRetryOptions.timeUnit);
        if (connection != null && checkConnection) {
            if (!connection.isValid(connectionValidationTimeOut)) {
                handleInvalidConnection(connection);
                connection = null;
            }
        }
        return connection;
    }
    
    private void handleInvalidConnection(Connection con) {
        Try.of(con::close);
        aquireDbConnection(dbUrl);
    }

    private void aquireDbConnection(String dbUrl) {
        Retry.of(() -> DriverManager.getConnection(dbUrl))
        .maxTries(poolRetryOptions.numOfRetries)
        .delay(poolRetryOptions.delay, poolRetryOptions.timeUnit)
        .withErrorHandler((i, j, th) -> System.out.println(th))
        .retry()
        .thenAccept(c -> pool.add(new PooledConnection(c)));
    }

    /**
     * Retry options in terms of number of retries, delays and time units.
     */
    public static class RetryOptions {
        final long numOfRetries;
        final long delay;
        final TimeUnit timeUnit;
        
        public RetryOptions(long num, long delay, TimeUnit unit) {
            numOfRetries = num;
            this.delay = delay;
            timeUnit = unit;
        }
    }
    
    /**
     * Wrapper class for physical DB connection.
     * 
     * Delegates calls to the physical connection.
     * Overrides {@link #close()} method to return connection to the pool.
     * 
     * @author skopylov@gmail.com
     *
     */
    class PooledConnection implements Connection {
        
        private final Connection delegate;

        /**
         * Constructor
         * @param c physical DB connection
         */
        PooledConnection(Connection c) {
            delegate = c;
        }
        
        /**
         * Gets physical connection
         * @return physical connection
         */
        public Connection getDelegate() {
            return delegate;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }

        @Override
        public Statement createStatement() throws SQLException {
            return delegate.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return delegate.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return delegate.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return delegate.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            delegate.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return delegate.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            delegate.commit();
        }

        @Override
        public void rollback() throws SQLException {
            delegate.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (checkOrphans) {
                Tuple<Instant,StackTraceElement[]> remove = orphaned.remove(this);
                if (remove == null) {
                    throw new IllegalStateException();
                }
            }
            pool.add(this);
        }

        @Override
        public boolean isClosed() throws SQLException {
            return delegate.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return delegate.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            delegate.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return delegate.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            delegate.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return delegate.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            delegate.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return delegate.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return delegate.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            delegate.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return delegate.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            delegate.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            delegate.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return delegate.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return delegate.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return delegate.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            delegate.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            delegate.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
                throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return delegate.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return delegate.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return delegate.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return delegate.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return delegate.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return delegate.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return delegate.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return delegate.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            delegate.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            delegate.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return delegate.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return delegate.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return delegate.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return delegate.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            delegate.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return delegate.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            delegate.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            delegate.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return delegate.getNetworkTimeout();
        }
        
    }
    
}
