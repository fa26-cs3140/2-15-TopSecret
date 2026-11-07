package org.model.messaging;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.managers.*;
import org.model.UserAccount;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class SqliteMessageStoreIntegrationTest {
 @TempDir Path dir;
 private SqliteMessageStore store;
 private String url;
 @BeforeEach void setup(){ url="jdbc:sqlite:"+dir.resolve("test.db");new SqliteUserAccountStore(url).addUser(new UserAccount("sender",false));
  var facilities=new FacilityManager(url);facilities.createTable(); facilities.addRecord("HQ","HQ");
  var agents=new AgentManager(url);agents.createTable();agents.addRecord("Ghost","1990-01-01",null,null,1);store=new SqliteMessageStore(url); }
 @Test void roundTripAndIsolation(){var m=store.save(new Message(0,"sender",1,"Ghost","O'Brien\nMeet at dawn",LocalDateTime.now()));
  assertTrue(m.getId()>0);var inbox=new SqliteMessageStore(url).findForRecipient("gHoSt");assertEquals(m.getBody(),inbox.get(0).getBody());
  assertEquals(m.getSentAt(),inbox.get(0).getSentAt());assertTrue(store.findForRecipient("other").isEmpty());assertTrue(store.findForRecipient("' OR 1=1 --").isEmpty()); }
 @Test void invalidWrites(){assertThrows(IllegalArgumentException.class,()->store.save(null));
  assertThrows(IllegalArgumentException.class,()->store.save(new Message(1,"sender",1,"Ghost","Hi",LocalDateTime.now())));
  assertThrows(IllegalArgumentException.class,()->store.save(new Message(0,"sender",2,"Ghost","Hi",LocalDateTime.now())));
  assertThrows(IllegalArgumentException.class,()->store.save(new Message(0,"sender",1,"Wrong","Hi",LocalDateTime.now())));
  assertThrows(IllegalStateException.class,()->store.save(new Message(0,"unknown",1,"Ghost","Hi",LocalDateTime.now())));
  assertThrows(IllegalArgumentException.class,()->store.findForRecipient(null));assertThrows(IllegalArgumentException.class,()->new SqliteMessageStore(" ")); }
}
