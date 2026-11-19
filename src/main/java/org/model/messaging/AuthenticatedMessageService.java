package org.model.messaging;

import org.managers.AgentManager;
import org.managers.UserAccountStore;
import org.model.Agent;
import org.model.UserAccount;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Messaging service bound to one authenticated user's account.
 *
 * Construct this service only after the application's login flow has
 * authenticated the user. The username is captured at construction time,
 * so later changes to the UserAccount object cannot change the identity.
 * Admin accounts have no special access to other users' messages.
 */
public final class AuthenticatedMessageService implements MessageService {

    private final AgentManager agents;
    private final UserAccountStore users;
    private final MessageStore store;
    private final String username;

    public AuthenticatedMessageService(
            AgentManager agents,
            UserAccountStore users,
            MessageStore store,
            UserAccount authenticatedUser) {

        if (agents == null || users == null || store == null || authenticatedUser == null
                || authenticatedUser.getUsername() == null) {
            throw new IllegalArgumentException("Authenticated messaging dependencies required.");
        }

        this.agents = agents;
        this.users = users;
        this.store = store;
        this.username = authenticatedUser.getUsername();
        requireAccount();
    }

    /**
     * Helper: Checks that the account still exists and its identity is unambiguous.
     */
    private void requireAccount() {
        if (users.getUser(username) == null) {
            throw new IllegalArgumentException("Authenticated account no longer exists.");
        }

        long matchingAccounts = users
                .getUsers()
                .stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .count();

        if (matchingAccounts != 1) {
            throw new IllegalStateException("Ambiguous account identity. Resolve case-duplicate accounts before messaging.");
        }
    }

    @Override
    public List<Agent> getAgents() {
        requireAccount();
        return List.copyOf(agents.getRecords("id", "%"));
    }

    @Override
    public Message sendMessage(int agentId, String body) {
        requireAccount();

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }

        Agent recipient = findAgentById(agentId);
        Message message = new Message(
                0,
                username,
                agentId,
                recipient.getName(),
                body,
                LocalDateTime.now());

        return store.save(message);
    }

    /** Helper: Finds a valid recipient in the current list of agents. */
    private Agent findAgentById(int agentId) {
        return getAgents().stream()
                .filter(agent -> agent.getId() == agentId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such agent."));
    }

    @Override
    public List<Message> getInbox() {
        requireAccount();
        return store.findForRecipient(username);
    }
}
