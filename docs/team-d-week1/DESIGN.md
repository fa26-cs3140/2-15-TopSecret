# Week 1 messaging design and implementation notes

## Status and MVC contracts

Implemented now: immutable Message snapshot, MessageController delegation
scaffold, service/store/view interfaces, proposed SQL resource and focused tests.
No live messaging service, SQLite message repository, terminal prompts or HTTP
message routes exist. The SQL resource is packaged but not applied at startup.

- model.messaging.Message: id (long), senderUsername, recipientAgentId (int),
  recipientName, body and sentAt (LocalDateTime). ID 0 is unsaved; negative IDs,
  nonpositive agent IDs, blank/null text and absent timestamp are rejected.
  Fields are immutable snapshots with getters; setters would weaken the contract.
- model.messaging.MessageStore: save(Message) returns a generated-ID snapshot;
  findForRecipient(name) returns case-insensitive matches ordered by generated ID.
  It is a low-level persistence boundary, not a public authorization API.
- model.messaging.MessageService: getAgents(), sendMessage(agentId, body),
  getInbox(). Future instances bind to a verified session on construction.
  No UI action can submit an alternative sender or inbox identity.
- controller.messaging.MessageController: listAgents(), sendMessage(), showInbox()
  delegate to the service and view. Confirmation follows successful service return.
  Validation errors go to showError. Storage failures propagate for the future
  top-level terminal handler; they never produce a success confirmation.
- view.messaging.MessageView: showAgents, showInbox, showMessageSent, showError.
  This rendering boundary preserves the existing terminal/web team's ownership.

All packages are under org.example. Existing application packages are preserved;
the whole-repository MVC refactor belongs to the shared team integration.

## Proposed database addition

The authoritative executable DDL is the resource
org/example/model/messaging/schema.sql. It is additive and idempotent, and assumes
existing users and agents tables. The integration tests construct those tables
using the real SqliteUserAccountStore and SqliteAgencyDataService.

| agent_messages column | SQLite definition / meaning |
| --- | --- |
| id | INTEGER PRIMARY KEY AUTOINCREMENT; send order |
| sender_username | TEXT NOT NULL REFERENCES users(username); verified sender |
| recipient_agent_id | INTEGER NOT NULL REFERENCES agents(id); existing target |
| recipient_name | TEXT NOT NULL COLLATE NOCASE; agent-name snapshot; nonblank |
| body | TEXT NOT NULL; nonblank including ordinary whitespace-only messages |
| sent_at | TEXT NOT NULL; service-generated LocalDateTime ISO string |

Index idx_agent_messages_recipient covers (recipient_name COLLATE NOCASE, id).
All writes must use prepared statements and enable PRAGMA foreign_keys=ON on
every connection. Foreign keys retain SQLite's default NO ACTION deletion
behavior. There is no cascading message deletion and no recipient-user foreign
key: the recipient account may not exist yet. No password fields are added.

Proposed inbox query:

```sql
SELECT id, sender_username, recipient_agent_id, recipient_name, body, sent_at
FROM agent_messages
WHERE recipient_name = ? COLLATE NOCASE
ORDER BY id;
```

The parameter must come from the verified session, never user-entered inbox
selection. SQL filtering alone is not authorization. SQLite NOCASE is ASCII
case-insensitive, consistent with the current HW5 ASCII username rules.
The future service must snapshot the actual agent name after resolving the ID,
validate account identity ambiguity, and validate timestamps. The DDL alone
cannot verify that a supplied snapshot name belongs to its agent ID or that
arbitrary timestamp text is valid. Those Week 2 behaviors are not claimed tested.

## Shared existing schema represented in the ERD

users: username TEXT NOT NULL PRIMARY KEY; is_admin INTEGER NOT NULL DEFAULT 0.
audit_log: id INTEGER PRIMARY KEY AUTOINCREMENT; username TEXT NOT NULL REFERENCES
users(username); action, content_type, content_identifier, accessed_at TEXT NOT NULL.
facilities: id INTEGER PRIMARY KEY; name and abbreviation TEXT NOT NULL.
agents: id INTEGER PRIMARY KEY; name/date_of_birth TEXT NOT NULL;
date_of_death/notes TEXT nullable; facility_id INTEGER NOT NULL REFERENCES facilities(id).
briefs: id INTEGER PRIMARY KEY; title/date/text TEXT NOT NULL; mission_id INTEGER UNIQUE.
The present briefs DDL has no mission foreign key, and the Mission Java model
is not evidence that a missions/join table is already persisted. Team A owns that
schema extension; this Team D ERD does not invent implemented tables for it.

## Unresolved Week 2 identity policies

The requirements explicitly make agent-name matching case-insensitive. HW5 can
store two distinct usernames differing only by case. Deny ambiguous inbox access
until the account/model owners agree on a uniqueness or alias policy; do not
quietly let both users read private messages. The acceptance draft requires this
fail-closed behavior. Ghost is compatible with current username rules; names
with spaces/punctuation need coordinated mapping or compatible test data.
Recipient-name snapshotting is a proposed policy: renaming the agent does not
silently reassign historical messages. Confirm this with teammates.

## Earlier Team D work reviewed

History identifies HW4 terminal work and HW5 account/audit work. Those features,
tests and homework-specific documentation remain relevant, so no old Team D
source or documentation was removed as redundant. A full-repository cache/binary
cleanup would affect teammates and is not included. The pre-existing untracked
MathServer scratch files and prior report remain outside the new commits.

## Artifacts and team discussion

Class diagram: current interfaces/scaffold solid, future implementations dashed.
ERD: existing shared tables solid, new tested-but-unapplied schema dashed.
Wireframes: terminal compose/inbox flow and a proposed plain-HTML read-only flow
for discussion with Teams B/C. They are design proposals, not team consensus.
Message bodies and inboxes are deliberately absent from the web wireframe.
Team discussion and final joint review/Gradescope submission require the humans.
