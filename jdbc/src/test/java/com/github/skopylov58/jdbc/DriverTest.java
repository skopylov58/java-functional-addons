package com.github.skopylov58.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;

import junit.framework.TestCase;

public class DriverTest extends TestCase {

    static final String h2 = "jdbc:h2:mem:test_mem";
    static final String url = "jdbc:middleman:" + h2;

    static final String createTable = "CREATE TABLE NAMES (name VARCHAR(255), age INT)";
    static final String dropTable = "DROP TABLE NAMES";

    public void testGetConnection() throws Exception {
        Connection connection = DriverManager.getConnection(url);
        
        String schema = connection.getSchema();
        System.err.println(schema);
        connection.isClosed();
        connection.getAutoCommit();
        connection.createClob();
        Statement statement = connection.createStatement();
        statement.close();
        connection.close();
        
    }
    
    public void testSqlStatement() throws Exception {
        
        String insertData  = "INSERT INTO NAMES (name, age) VALUES ('bob', 30)";
        
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute(createTable);
            statement.execute(insertData);
            statement.execute(dropTable);
        }
        System.out.println("Completed");
        
    }

    public void testSqlError() throws Exception {
        String insertData  = "INSERT INTO NAMES (name, ages) VALUES ('bob', 30)";
        
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute(createTable);
            statement.execute(insertData);
            connection.commit();
            statement.execute(dropTable);
            fail();
        } catch (Exception e) {
            //ok
        }
        System.out.println("Completed");
        
    }

    public void testSqlPreparedStatement() throws Exception {
        String insertData  = "INSERT INTO NAMES (name, age) VALUES (?, ?)";
        String selectData  = "SELECT * from NAMES";
        
        try (Connection connection = DriverManager.getConnection(url)) {
            Statement statement = connection.createStatement();
            statement.execute(createTable);
            PreparedStatement insertStatement = connection.prepareStatement(insertData);
            insertStatement.setString(1, "John");
            insertStatement.setInt(2, 40);
            insertStatement.executeUpdate();
            
            ResultSet query = statement.executeQuery(selectData);
            while(query.next()) {
                String name = query.getString(1);
                int age = query.getInt(2);
                System.out.println(name + " " + age);
            }
            
            statement.execute(dropTable);
        }
    }

    public void testDrivers() throws Exception {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while(drivers.hasMoreElements()) {
            System.err.println(drivers.nextElement());
        }
    }
}
