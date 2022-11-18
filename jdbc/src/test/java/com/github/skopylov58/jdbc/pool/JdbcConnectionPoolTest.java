package com.github.skopylov58.jdbc.pool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class JdbcConnectionPoolTest {

    
    @Test
    public void test0() throws Exception {
        String h2 = "jdbc:h2:mem:test_mem";
        JDBCConnectionPool pool = new JDBCConnectionPool(h2);
        pool.start();
        Connection connection = pool.getConnection();
        assertNotNull(connection);
        connection.close();
        pool.stop();
    }

    @Test
    public void test1() throws Exception {
        String h2 = "jdbc:h2:mem:test_mem";
        JDBCConnectionPool pool = new JDBCConnectionPool(h2, 2);
        pool.start();
        
        Connection connection = pool.getConnection();
        assertNotNull(connection);
        
        Connection connection2 = pool.getConnection();
        assertNotNull(connection2);

        try {
            Connection connection3 = pool.getConnection();
            fail();
        } catch (SQLException e) {
            //expected
            System.out.println(e.getMessage());
        }
        
        connection.close();
        connection2.close();
        pool.stop();
    }

    
    void insert(Connection c) {
        try (c) {
            
        } catch (SQLException sqle) {
            
        }
    }
    
    
    void usage() {
        CompletableFuture<Connection> f = null;
        f.orTimeout(1, TimeUnit.SECONDS)
        .thenAccept(this::insert)
        .whenComplete((v, t) -> System.out.println(t));
    }
    
    
    
}
