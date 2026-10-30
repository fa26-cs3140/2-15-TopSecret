package org.example.model.messaging;

import java.util.List;

/** Week 1 persistence contract; the SQLite adapter is Week 2 work.
 * This low-level interface does not authorize callers; do not expose it to views.
 */
public interface MessageStore {
    /** Save an unsaved snapshot (ID zero); return a snapshot with its generated ID. */
    Message save(Message message);

    /** ASCII case-insensitive name matching, ascending generated ID order. */
    List<Message> findForRecipient(String recipientName);
}
