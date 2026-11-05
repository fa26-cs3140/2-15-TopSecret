package org.model.messaging;

import org.model.Agent;
import java.util.List;

/** Session-bound service contract. The implementation is bound to ONE authenticated
 * session when constructed; a caller cannot supply a sender or inbox username.
 * Creating an unauthenticated instance must be rejected. All accounts, including
 * admins, receive only their own messages. Ambiguous case-insensitive account
 * identities must fail closed. AuthenticatedMessageService supplies the live implementation.
 */
public interface MessageService {
    List<Agent> getAgents();

    /** Resolve an existing agent by ID; snapshot its name and the authenticated
     * sender; reject missing agents and null/blank bodies. Recipient account may
     * be created later. Return only after persistence succeeds.
     */
    Message sendMessage(int recipientAgentId, String body);

    /** Match the authenticated username to snapshotted agent name ignoring ASCII
     * case. No admin override; no UI-supplied recipient. Ordered oldest ID first.
     */
    List<Message> getInbox();
}
