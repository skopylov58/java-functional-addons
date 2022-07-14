package com.github.skopylov58.jdbc.pool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import com.github.skopylov58.jdbc.pool.JDBCConnectionPool;

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

    
    
}