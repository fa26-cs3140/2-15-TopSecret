package org.controller.messaging;
import org.junit.jupiter.api.Test;
import org.managers.*;
import org.model.*;
import org.model.messaging.*;
import org.view.AgencyInterface;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TerminalMessagingIntegrationTest {
 private String run(String input,MessageService messages){var out=new ByteArrayOutputStream();var ui=new AgencyInterface(mock(AgentManager.class),mock(FacilityManager.class),mock(BriefManager.class),mock(UserAccountManager.class),mock(AuditLogService.class),new UserAccount("sender",false),new Scanner(input),new PrintStream(out));ui.enableMessaging(messages);ui.run();return out.toString();}
 @Test void sendsMultilineAndDisplaysOwnInbox(){var s=mock(MessageService.class);when(s.getAgents()).thenReturn(List.of(new Agent(1,"Ghost","1990-01-01",null,null,1)));
  var m=new Message(1,"sender",1,"Ghost","line one\nline two",LocalDateTime.now());when(s.sendMessage(1,m.getBody())).thenReturn(m);when(s.getInbox()).thenReturn(List.of(m));
  String output=run("8\n1\nline one\nline two\n.\n9\n0\n",s);assertTrue(output.contains("Message sent to Ghost"));assertTrue(output.contains(m.getBody()));verify(s).sendMessage(1,m.getBody());}
 @Test void cancelledOrIncompleteInputNeverSends(){var s=mock(MessageService.class);when(s.getAgents()).thenReturn(List.of());run("8\n1\n/cancel\n0\n",s);run("8\n1\npartial\n",s);verify(s,never()).sendMessage(anyInt(),anyString());}
 @Test void reportsStorageFailureWithoutClaimingSuccess(){var s=mock(MessageService.class);when(s.getAgents()).thenReturn(List.of());when(s.sendMessage(1,"hello")).thenThrow(new IllegalStateException());assertFalse(run("8\n1\nhello\n.\n0\n",s).contains("Message sent"));}
}
