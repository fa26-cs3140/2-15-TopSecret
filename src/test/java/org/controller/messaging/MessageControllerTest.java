package org.controller.messaging;

import org.model.Agent;
import org.model.messaging.Message;
import org.model.messaging.MessageService;
import org.view.messaging.MessageView;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageControllerTest {
    private final MessageService service = mock(MessageService.class);
    private final MessageView view = mock(MessageView.class);
    private final MessageController controller = new MessageController(service, view);

    @Test
    void constructorRequiresBothDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new MessageController(null, view));
        assertThrows(IllegalArgumentException.class, () -> new MessageController(service, null));
    }

    @Test
    void listsTheModelAgentsWithoutReadingInputOrSql() {
        var agents = List.of(new Agent(1, "Ghost", "1990-01-01", null, null, 1));
        when(service.getAgents()).thenReturn(agents);
        controller.listAgents();
        verify(view).showAgents(agents);
        verifyNoMoreInteractions(view);
    }

    @Test
    void confirmsOnlyTheMessageReturnedAfterSuccessfulPersistence() {
        var sent = new Message(4, "sender", 1, "Ghost", "Meet at dawn", LocalDateTime.now());
        when(service.sendMessage(1, "Meet at dawn")).thenReturn(sent);
        controller.sendMessage(1, "Meet at dawn");
        var order = inOrder(service, view);
        order.verify(service).sendMessage(1, "Meet at dawn");
        order.verify(view).showMessageSent(sent);
        verifyNoMoreInteractions(view);
    }

    @Test
    void validationFailureDoesNotClaimDelivery() {
        when(service.sendMessage(99, "hello")).thenThrow(new IllegalArgumentException("No such agent."));
        controller.sendMessage(99, "hello");
        verify(view).showError("No such agent.");
        verifyNoMoreInteractions(view);
    }

    @Test
    void inboxUsesOnlyTheAuthenticatedServiceAndHandlesAnEmptyList() {
        when(service.getInbox()).thenReturn(List.of());
        controller.showInbox();
        verify(service).getInbox();
        verify(view).showInbox(List.of());
        verifyNoMoreInteractions(service, view);
    }

    @Test
    void storageFailureDoesNotClaimDeliveryOrHideTheFailure() {
        when(service.sendMessage(1, "hello")).thenThrow(new IllegalStateException("Storage unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.sendMessage(1, "hello"));
        verifyNoInteractions(view);
    }


    @Test
    void showInboxReturnsMessagesForSelection() {
        Message unread = new Message(
                5, "Ghost", 1, "sender",
                "Meet at HQ", LocalDateTime.now(), false
        );

        List<Message> inbox = List.of(unread);
        when(service.getInbox()).thenReturn(inbox);

        List<Message> result = controller.showInbox();

        // The controller returns the same inbox for terminal selection.
        assertEquals(inbox, result);

        // Listing should not mark anything as read.
        verify(service).getInbox();
        verify(view).showInbox(inbox);
        verify(service, never()).readMessage(anyLong());
    }

    @Test
    void readMessageDisplaysTheOpenedMessage() {
        Message read = new Message(
                5, "Ghost", 1, "sender",
                "Secret mission", LocalDateTime.now(), true
        );

        when(service.readMessage(5)).thenReturn(read);

        controller.readMessage(5);

        // The service opens the message before the view displays it.
        var order = inOrder(service, view);

        order.verify(service).readMessage(5);
        order.verify(view).showMessageDetail(read);

        verifyNoMoreInteractions(service, view);
    }

    @Test
    void readMessageFailureDoesNotDisplayMessage() {
        when(service.readMessage(99)).thenThrow(new IllegalArgumentException("Message not found in your inbox."));

        assertThrows(IllegalArgumentException.class, () -> controller.readMessage(99));

        // An unauthorized or nonexistent message must not be displayed.
        verify(service).readMessage(99);
        verifyNoInteractions(view);
    }
}
