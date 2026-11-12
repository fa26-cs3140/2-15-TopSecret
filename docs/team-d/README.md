# Team D model and access control

Homework 7 assigns Team D the model, related records, terminal search, audit
logging for record changes, and web session administration. This branch covers
the Week 1 review, design, tests, and stubs. The shared checkpoint starts at
`568d4ee` on `NewDatabaseManagerRefactor`.

- [Prioritized backlog and team handoff](WORK-PLAN.md)
- [Class diagram, ERD, and wireframes](DESIGN.md)
- [Verification and system test checklist](VERIFICATION.md)

## Current behavior

A browser signs in through the existing web login. Its cookie is still a
browser-session cookie. The server expires the token after 30 minutes of
inactivity, and a request with an active token refreshes its last activity.
Logout and revocation remove the token. Session state lives in the web server
process and is cleared on restart. Administrator checks on session listing and
revocation are in `SessionManager`; the terminal management menu is planned
for Week 2.

In the terminal, **9. My messages** opens the authenticated account's inbox
and **10. Send an agent message** sends to an agent. Enter `.` on its own line
to send or `/cancel` to cancel. Recipient names are matched against the
authenticated username, ignoring case. Team C owns the web messaging center
and its read indicators for Homework 7.

## Data boundaries

- `UserAccountManager` uses SQLite for account metadata and per-user
  `credentials-<hex username>.cip` files for passwords. Usernames follow
  `[a-z0-9_-]{3,}`; passwords require at least five characters with both
  uppercase and lowercase letters.
- `AuditLogManager` and `SqliteAuditLogStore` preserve existing READ and
  CREATE entries. The planned edit store uses the same audit schema and
  commits the content change and audit row together.
- `RecordRelationships` resolves current links using the managers and mission
  junction tables. `RecordSearch` searches the current content models with
  literal, case-insensitive matching. Both return typed results for the view
  to render.
- `RecordEditor<T>` is the Week 1 boundary for authenticated edits. Its store
  contract is tested with Mockito. Week 2 supplies the SQLite implementation
  and connects it to the web forms.
