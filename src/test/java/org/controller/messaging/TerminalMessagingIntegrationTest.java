package org.controller.messaging;

import org.junit.jupiter.api.Test;
import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
import org.model.Agent;
import org.model.UserAccount;
import org.model.messaging.Message;
import org.model.messaging.MessageService;
import org.view.AgencyInterface;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalMessagingIntegrationTest {

    @Test
    void displayedOptionsSendMultilineAndDisplayInbox() {
        MessageService service = mock(MessageService.class);
        when(service.getAgents()).thenReturn(List.of(new Agent(1, "Ghost", "1990-01-01", null, null, 1)));
        Message message = new Message(1, "sender", 1, "Ghost", "line one\nline two", LocalDateTime.now());
        when(service.sendMessage(1, message.getBody())).thenReturn(message);
        when(service.getInbox()).thenReturn(List.of(message));

        String output = run("10\n1\nline one\nline two\n.\n9\n0\n", service);

        assertTrue(output.contains("9. My messages\n10. Send an agent message"));
        assertTrue(output.contains("Message sent to Ghost"));
        assertTrue(output.contains(message.getBody()));
        verify(service).sendMessage(1, message.getBody());
        verify(service).getInbox();
    }

    @Test
    void cancelledOrIncompleteInputNeverSends() {
        MessageService service = mock(MessageService.class);

        run("10\n1\n/cancel\n0\n", service);
        run("10\n1\npartial\n", service);

        verify(service, times(2)).getAgents();
        verify(service, never()).sendMessage(anyInt(), anyString());
    }

    @Test
    void reportsStorageFailureWithoutClaimingSuccess() {
        MessageService service = mock(MessageService.class);
        when(service.sendMessage(1, "hello")).thenThrow(new IllegalStateException());

        String output = run("10\n1\nhello\n.\n0\n", service);

        verify(service).sendMessage(1, "hello");
        assertTrue(output.contains("Messaging unavailable. No delivery was confirmed."));
        assertFalse(output.contains("Message sent"));
    }

    private String run(String input, MessageService messages) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgencyInterface ui = new AgencyInterface(
                mock(AgentManager.class), mock(FacilityManager.class), mock(BriefManager.class),
                mock(UserAccountManager.class), mock(MissionManager.class), mock(AuditLogService.class),
                new UserAccount("sender", false), new Scanner(input), new PrintStream(output));
        ui.enableMessaging(messages);
        ui.run();
        return output.toString();
    }
}
