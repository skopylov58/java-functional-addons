package com.github.skopylov58.retry.samples;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.skopylov58.functional.Try;
import com.github.skopylov58.retry.Retry;
import com.github.skopylov58.retry.Retry;

/**
 * Example to execute JDBC statements with Try
 * @author skopylov@gmail.com
 *
 */
public class JDBCTest {
   private static final String DB_URL_H2 = "jdbc:h2:mem:myDb;DB_CLOSE_DELAY=-1";
   private static final String USER = "sa";
   private static final String PASWD = "sa";
   private static final String QUERY = "SELECT 'Hello, Try'";
   
   private final String [] DB_URLS = {
           "jdbc:mysql:foo",
           "jdbc:oracle:bar",
           DB_URL_H2,
   };

   @Test
   public void testJDBC() {
       List<String> list = JDBCWithTry();
       assertEquals(1, list.size());
       assertEquals("Hello, Try", list.get(0));

       list = JDBCTraditional();
       assertEquals(1, list.size());
       assertEquals("Hello, Try", list.get(0));
   }
   
   public List<String> JDBCWithTry() {
       return Try.of(() -> getConnectionWithRetry(DB_URLS).get())
       .map(Connection::createStatement)
       .map(s -> s.executeQuery(QUERY))
       .map(JDBCTest::processResultSet)
       //.logException()
       .optional()
       .orElse(Collections.emptyList());
   }
   
   public List<String> JDBCTraditional() {
       try (Connection con = getConnectionWithRetry(DB_URLS).get();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(QUERY)) {
           return processResultSet(rs);
       } catch (Exception e) {
           System.out.println(e.getMessage());
           return Collections.emptyList();
       }       
   }
   
   //Unfortunately there is no good functional API for ResultSet
   static List<String> processResultSet(ResultSet rs) throws SQLException {
       List<String> res = new LinkedList<>();
       while(rs.next()) {
           res.add(rs.getString(1));
       }
       return res;
   }
   
   /**
    * Connects to first alive database.
    * 
    * @param jdbcUrls list of databases to connect
    * @return connection to first alive database
    */
   public Connection getConnection(String [] jdbcUrls) throws SQLException {
       return Stream.of(jdbcUrls)
       .map(url -> Try.of(() -> DriverManager.getConnection(url, USER, PASWD)))
       .flatMap(Try::stream)
       .findFirst()
       .get();
   }
   
   public CompletableFuture<Connection> getConnectionWithRetry(String [] jdbcUrls) {
       return Retry.of(() -> getConnection(jdbcUrls)).retry();
   }
}