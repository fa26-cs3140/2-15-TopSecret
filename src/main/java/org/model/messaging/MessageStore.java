package org.model.messaging;

import java.util.List;

/** Persistence contract implemented by SqliteMessageStore.
 * This low-level interface does not authorize callers; do not expose it to views.
 */
public interface MessageStore {
    /** Save an unsaved snapshot (ID zero); return a snapshot with its generated ID. */
    Message save(Message message);

    /** ASCII case-insensitive name matching, ascending generated ID order. */
    List<Message> findForRecipient(String recipientName);
}
