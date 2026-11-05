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
   if(f.getRecords("id","%").isEmpty())f.addRecord("HQ","HQ");agents=new AgentManager(url);agents.createTable();messages=new SqliteMessageStore(url);}
  public void addAccount(String name,boolean admin){if(!users.addUser(new UserAccount(name,admin)))throw new IllegalStateException("Cannot seed user");}
  public int addAgent(String name){return agents.addRecord(name,"1990-01-01",null,null,1).getId();}
  public MessageService sessionFor(String name){return new AuthenticatedMessageService(agents,users,messages,users.getUser(name));}
  public Fixture reopen(){return new RealFixture(url);}
 }
}
