package org.example.model.messaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** DRAFT acceptance suite: compile-only in Week 1. Extend this abstract contract
 * with a real SQLite/authenticated-service fixture in Week 2. No tests here run
 * until that implementation exists; no fake service or placeholder passing test.
 */
public abstract class MessagingAcceptanceContract {
    protected interface Fixture {
        void addAccount(String username, boolean admin);
        int addAgent(String name);
        MessageService sessionFor(String authenticatedUsername);
        /** Reconstruct services/stores over the same persisted data. */
        Fixture reopen();
    }
    protected abstract Fixture fixture();

    @Test
    void messageCanPrecedeRecipientAccountCreation() {
        var f = fixture(); f.addAccount("sender", false); int ghost = f.addAgent("Ghost");
        f.sessionFor("sender").sendMessage(ghost, "Meet at dawn");
        f.addAccount("Ghost", false);
        assertEquals("Meet at dawn", f.sessionFor("Ghost").getInbox().get(0).getBody());
    }

    @Test
    void usernameAndAgentNameMatchIgnoringCase() {
        var f = fixture(); f.addAccount("sender", false); f.addAccount("gHoSt", false);
        f.sessionFor("sender").sendMessage(f.addAgent("Ghost"), "Private");
        assertEquals(1, f.sessionFor("gHoSt").getInbox().size());
    }

    @Test
    void neitherAnotherUserNorAnAdminCanReadGhostsInbox() {
        var f = fixture(); f.addAccount("sender", false); f.addAccount("Ghost", false);
        f.addAccount("other", false); f.addAccount("admin", true);
        f.sessionFor("sender").sendMessage(f.addAgent("Ghost"), "Private");
        assertEquals(1, f.sessionFor("Ghost").getInbox().size());
        assertTrue(f.sessionFor("other").getInbox().isEmpty());
        assertTrue(f.sessionFor("admin").getInbox().isEmpty());
        assertTrue(f.sessionFor("sender").getInbox().isEmpty());
    }

    @Test
    void invalidTargetsAndBodiesAreRejectedWithoutSaving() {
        var f = fixture(); f.addAccount("sender", false); f.addAccount("Ghost", false);
        int ghost = f.addAgent("Ghost"); var service = f.sessionFor("sender");
        for (int id : new int[]{0, -1, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> service.sendMessage(id, "Hello"));
        }
        for (String body : new String[]{null, "", " ", "\t\n"}) {
            assertThrows(IllegalArgumentException.class, () -> service.sendMessage(ghost, body));
        }
        assertTrue(f.sessionFor("Ghost").getInbox().isEmpty());
    }

    @Test
    void senderAndTargetAreResolvedFromSessionAndAgentData() {
        var f = fixture(); f.addAccount("sender", false); int ghost = f.addAgent("Ghost");
        var sent = f.sessionFor("sender").sendMessage(ghost, "Hello");
        assertTrue(sent.getId() > 0); assertEquals("sender", sent.getSenderUsername());
        assertEquals(ghost, sent.getRecipientAgentId()); assertEquals("Ghost", sent.getRecipientName());
        assertEquals("Hello", sent.getBody()); assertNotNull(sent.getSentAt());
    }

    @Test
    void inboxPersistsInSendOrderAcrossSessions() {
        var f = fixture(); f.addAccount("sender", false); f.addAccount("Ghost", false);
        int ghost = f.addAgent("Ghost");
        f.sessionFor("sender").sendMessage(ghost, "first");
        f.sessionFor("sender").sendMessage(ghost, "second");
        var inbox = f.reopen().sessionFor("Ghost").getInbox();
        assertEquals(2, inbox.size()); assertEquals("first", inbox.get(0).getBody());
        assertEquals("second", inbox.get(1).getBody());
        assertTrue(inbox.get(0).getId() < inbox.get(1).getId());
    }

    @Test
    void ambiguousCaseInsensitiveAccountIdentitiesFailClosed() {
        var f = fixture(); f.addAccount("sender", false); f.addAccount("Ghost", false);
        f.sessionFor("sender").sendMessage(f.addAgent("Ghost"), "Private");
        // The fixture permits legacy HW5 duplicates to exercise the ambiguity guard.
        f.addAccount("ghost", false);
        assertThrows(IllegalStateException.class, () -> f.sessionFor("Ghost").getInbox());
        assertThrows(IllegalStateException.class, () -> f.sessionFor("ghost").getInbox());
    }

    @Test
    void unauthenticatedOrUnknownSessionCannotBeConstructed() {
        var f = fixture();
        assertThrows(IllegalArgumentException.class, () -> f.sessionFor(null));
        assertThrows(IllegalArgumentException.class, () -> f.sessionFor("unknown"));
    }
}
