# Homework 7 model and session backlog

Reviewed the shared branch at `568d4ee` against the assignment. The baseline
passed 322 tests with 271 of 299 methods covered by JaCoCo. The green baseline
still had workflow gaps that existing tests did not exercise.

| Priority | Finding and action | Owner / state |
| --- | --- | --- |
| P0 | The terminal displayed Send and Inbox in reverse. Cancellation and storage-error tests still chose the password menu. Align labels with the existing actions and verify the tests reach messaging. | D, fixed |
| P0 | Browser tokens currently need a 30-minute idle limit and a way for admins to revoke them. Use the existing session manager and test the exact deadline and HTTP redirects with an injected clock. | D, expiry and manager operations tested |
| P0 | Record managers build legacy update SQL from string inputs. The web write path needs typed inputs, exact IDs, parameter binding, validation, and one transaction for the content change and audit row. | D, contract and unit tests ready; SQLite stores in Week 2 |
| P1 | A terminal process started separately from `-web` cannot see the running server's in-memory tokens. Add an opt-in launch mode that keeps the admin terminal and the web server in one process and injects the same session manager. | D with B, Week 2 wiring |
| P1 | Detail pages display plain text, including raw relation IDs. Supply typed related rows and render safe links. Keep the READ audit tied to the record actually opened. | D with B, model tested; HTML wiring in Week 2 |
| P1 | Terminal search was lost in the MVC refactor. Search all text properties of the current records and add the new country fields when Team A's model is available. | D, current fields tested; menu and country integration in Week 2 |
| P1 | Mission brief assignments are persisted in `j_mission_briefs`; `briefs.mission_id` is legacy. Keep one source for the new write/read paths and check existing data before any migration. Existing mission creation also needs a test for empty relation lists. | D with shared model owner, Week 2 |
| P2 | Web detail routing currently uses a plain-text not-found message and can return 200 for missing records. Give the view a typed result, return 404 for missing IDs, and share link rendering with list pages. | D with B, Week 2 |
| P2 | The older `SearchResult` class is unused, and some inherited test methods and classes lack coverage. Recheck call sites before removing dead code and use JaCoCo to prioritize remaining tests. | D / team, Week 2 |
| P2 | The documentation index still contains older teammate diagrams. Consolidate the current shared class diagram, ERD, and wireframe with their owners before submission. | Team, Week 2 |

## Integration agreements to review at check-in

- **Team A:** Confirm the country model, display label, route, and facility
  foreign key before adding them to the relationship and search contracts.
  Preserve existing facilities when migrating. Do not invent country IDs.
- **Team B:** Pass validated model values to `RecordEditor` and use the actor
  supplied by authenticated session lookup. The editor returns a generated
  ID on creation and false for a missing update target. Render `RelatedRecord`
  values as escaped links built from the record type and integer ID.
- **Team C:** Web messages and read flags use the existing recipient privacy
  rules. Keep message data out of record search and record-edit auditing.
- **Team D:** Use the current web process's session manager for terminal list
  and revoke. Require a current admin account at the terminal action.
  Recheck current account permissions in the web auth path before allowing
  edits, so a revoked admin role does not survive in a cached session account.
  Keep tokens out of HTML, SQLite, and audit entries.

## Week 2 order

1. Confirm the country contract, edit inputs, and launch mode with A and B.
2. Implement and test the transactional SQLite write path before enabling web
   forms. Verify no audit row or partial relationship update survives failure.
3. Wire related links, terminal search, and the terminal session commands.
4. Run the system checklist, close uncovered methods, and consolidate the
   shared documentation before submission.
