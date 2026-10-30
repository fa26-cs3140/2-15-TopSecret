package org.example.model.messaging;

import org.example.model.Agent;
import java.util.List;

/** Week 1 service contract. A future implementation is bound to ONE authenticated
 * session when constructed; a caller cannot supply a sender or inbox username.
 * Creating an unauthenticated instance must be rejected. All accounts, including
 * admins, receive only their own messages. Ambiguous case-insensitive account
 * identities must fail closed. No live implementation is supplied in Week 1.
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
