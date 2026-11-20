package org.model.messaging;
import org.managers.*;
import org.model.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
/** Runs every previously abstract Week 1 scenario against real SQLite. */
class MessagingAcceptanceIntegrationTest extends MessagingAcceptanceContract {
 @TempDir Path dir;
 @Override protected Fixture fixture(){return new RealFixture("jdbc:sqlite:"+dir.resolve("messages.db"));}
 private static class RealFixture implements Fixture {
  final String url;final SqliteUserAccountStore users;final AgentManager agents;final SqliteMessageStore messages;
  RealFixture(String url){this.url=url;users=new SqliteUserAccountStore(url);var f=new FacilityManager(url);f.createTable();
   if(f.getRecords("id","%").isEmpty())f.addRecord("HQ", "HQ", 1);agents=new AgentManager(url);agents.createTable();messages=new SqliteMessageStore(url);}
  public void addAccount(String name, boolean admin) {
   if (users.addUser(new UserAccount(name, admin))) return;
   // This acceptance scenario deliberately restores a legacy ambiguous identity.
   // Normal provisioning now rejects case variants; retain the legacy fail-closed check.
   try (var connection = java.sql.DriverManager.getConnection(url);
        var statement = connection.createStatement()) {
    statement.execute("DROP TRIGGER IF EXISTS users_unique_identity");
    try (var insert = connection.prepareStatement("INSERT INTO users (username, is_admin) VALUES (?, ?)")) {
     insert.setString(1, name);
     insert.setBoolean(2, admin);
     insert.executeUpdate();
    }
   } catch (java.sql.SQLException failure) {
    throw new IllegalStateException("Cannot seed legacy user", failure);
   } finally {
    new SqliteUserAccountStore(url); // Restore the production constraint after seeding.
   }
  }
  public int addAgent(String name){return agents.addRecord(name,"1990-01-01",null,null,1).getId();}
  public MessageService sessionFor(String name){return new AuthenticatedMessageService(agents,users,messages,users.getUser(name));}
  public Fixture reopen(){return new RealFixture(url);}
 }
}
