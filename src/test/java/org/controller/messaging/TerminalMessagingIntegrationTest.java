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
import static org.mockito.ArgumentMatchers.anyLong;

class TerminalMessagingIntegrationTest {

    @Test
    void sendsMultilineMessageUsingOptionNine() {
        MessageService service = mock(MessageService.class);

        when(service.getAgents()).thenReturn(List.of(
                new Agent(1, "Ghost", "1990-01-01", null, null, 1)
        ));

        String body = "line one\nline two";

        Message sent = new Message(
                1, "sender", 1, "Ghost", body, LocalDateTime.now()
        );

        when(service.sendMessage(1, body)).thenReturn(sent);

        String output = run("9\n1\nline one\nline two\n.\n0\n", service);

        assertTrue(output.contains("Message sent to Ghost"));
        verify(service).sendMessage(1, body);
    }

    @Test
    void selectingMessageMarksItAsRead() {
        MessageService service = mock(MessageService.class);

        Message unread = new Message(
                5,
                "Ghost",
                1,
                "sender",
                "Meet at HQ tomorrow",
                LocalDateTime.now(),
                false
        );

        Message read = new Message(
                unread.getId(),
                unread.getSenderUsername(),
                unread.getRecipientAgentId(),
                unread.getRecipientName(),
                unread.getBody(),
                unread.getSentAt(),
                true
        );

        when(service.getInbox()).thenReturn(List.of(unread));
        when(service.readMessage(5)).thenReturn(read);

        // 10 = My Messages, 1 = first message, 0 = exit.
        String output = run("10\n1\n0\n", service);

        assertTrue(output.contains("[Unread]"));
        assertTrue(output.contains("Meet at HQ tomorrow"));
        assertTrue(output.contains("Status: Read"));

        verify(service).getInbox();
        verify(service).readMessage(5);
    }

    @Test
    void cancelledOrIncompleteInputNeverSends() {
        MessageService service = mock(MessageService.class);

        run("9\n1\n/cancel\n0\n", service);
        run("9\n1\npartial\n", service);

        verify(service, times(2)).getAgents();
        verify(service, never()).sendMessage(anyInt(), anyString());
    }

    @Test
    void reportsStorageFailureWithoutClaimingSuccess() {
        MessageService service = mock(MessageService.class);
        when(service.sendMessage(1, "hello")).thenThrow(new IllegalStateException());

        String output = run("9\n1\nhello\n.\n0\n", service);

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


    @Test
    void returningFromInboxDoesNotMarkMessagesAsRead() {
        MessageService service = mock(MessageService.class);

        Message unread = new Message(
                5, "Ghost", 1, "sender",
                "Secret message", LocalDateTime.now(), false
        );

        when(service.getInbox()).thenReturn(List.of(unread));

        // Open inbox, immediately return, then exit application.
        String output = run("10\n0\n0\n", service);

        assertTrue(output.contains("[Unread]"));

        // The message body should not be displayed yet.
        assertFalse(output.contains("Secret message"));

        // No message was opened.
        verify(service, never()).readMessage(anyLong());
    }
}
