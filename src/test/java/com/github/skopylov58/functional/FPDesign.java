package com.github.skopylov58.functional;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;

public class FPDesign {

  class User {}
  class Connection {}
  
  
  class App {
    Function<Long, Optional<User>> getUser;
    
    App(Function<Long, Optional<User>> getUserFunc) {
      getUser = getUserFunc;
    }
    
    void doSomeBusinessStuff() {
      //...
      long userid = 1000;
      Optional<User> user  = getUser.apply(userid);
      //...
    }
  }

  
  class Main {
    
    void main_( ) {
      Connection con = new Connection();
      
      Function<Long, Optional<User>> getUser = id -> {
        try {
          User user = getUserFromDB(id, con);
          return Optional.of(user);
        } catch (SQLException e) {
          return Optional.empty();
        }
      };
      
      App app = new App(getUser);
      app.doSomeBusinessStuff();
    }
  }
  
  User getUserFromDB(long id, Connection con) throws SQLException {
    return new User();
  }
  
}
