package checks;

import java.sql.Connection;
import java.sql.Statement;

class AvoidDatabaseQueryInLoopCheck {

  interface UserMapper {
    String selectById(String id);
  }

  static class UserDao {
    String findById(String id) {
      return id;
    }
  }

  void jdbcQueryInsideFor(Connection c) throws Exception {
    for (int i = 0; i < 10; i++) {
      Statement s = c.createStatement();
      s.executeQuery("select 1"); // Noncompliant {{Avoid database queries inside loops (N+1). Fetch required data in bulk before the loop or batch queries.}}
    }
  }

  void jdbcQueryInsideWhile(Connection c) throws Exception {
    int i = 0;
    while (i++ < 10) {
      Statement s = c.createStatement();
      s.execute("select 1");
    }
  }

  void allowedOutsideLoop(Connection c) throws Exception {
    Statement s = c.createStatement();
    s.executeQuery("select 1");
  }

  void mapperAndDaoFallback(UserMapper mapper) {
    UserDao dao = new UserDao();
    for (int i = 0; i < 2; i++) {
      mapper.selectById(String.valueOf(i)); // Noncompliant {{Avoid database queries inside loops (N+1). Fetch required data in bulk before the loop or batch queries.}}
      dao.findById(String.valueOf(i)); // Noncompliant {{Avoid database queries inside loops (N+1). Fetch required data in bulk before the loop or batch queries.}}
    }
  }

  void allowedLoopWithoutDb() {
    for (int i = 0; i < 10; i++) {
      String v = String.valueOf(i);
      System.out.println(v);
    }
  }
}

