# Team D - Week 1: agent messaging

Branch: `team-d-agent-messaging`, branched from main at 09008ca.
Week 1 is test-first planning and scaffolding, not the completed Week 2 feature.
Do not merge into main until Week 2 integration and review. No push is planned.

## Ownership and requirements

Team D owns messages addressed to existing agents. A logged-in terminal user can
select an agent and compose a message. A later-created account can retrieve it
when its username matches that agent name, ignoring case. Only that identity
can read the message; administrator status does not confer inbox access.
Messages are not required in the web view. Preserve HW5 account/password/audit
code and Teams A/B/C's work. Use model, controller and view packages for new work.

## Week 1 deliverables

- Document the schema, MVC boundaries, access rules and acceptance cases.
- Add the immutable Message model and persistence/service/view interfaces.
- Add executable constructor/getter/validation tests and SQLite schema tests.
- Draft reusable executable acceptance contracts for the Week 2 implementation.
  Abstract contracts are compiled but deliberately not counted as passing tests.
- Supply PDF class/ERD diagrams and wireframes. Web wireframes are coordination
  proposals for the view owner, not Team D implementation or agreed team design.
- Verify scaffold tests and JAR packaging; record existing build blockers.

## Focused commit sequence

1. Document Team D's Week 1 scope and test plan.
2. Add tested Message model and service/persistence contracts.
3. Add schema validation, MVC scaffolding and acceptance-test drafts.
4. Add PDF diagrams, wireframes and verification/handoff notes.

Each commit represents one coherent change. No backdating, main merge or push.

## Week 2 handoff (not claimed complete)

Implement SqliteMessageStore and the authenticated, session-bound MessageService;
make the drafted acceptance contracts pass against the real implementation.
Connect the terminal controller/view to the authenticated application with the
control/view owners. Select the sender and inbox identity from the authenticated
session, never from editable user input. Add real session/persistence integration
and packaged-JAR smoke tests. Coordinate shared startup, seeded data and full
regression verification with the team before merging.

## Coordination decisions to confirm with teammates

- Use the same SQLite database as users and agents; create those tables first.
- Snapshot the agent name at send time so renaming an agent does not silently
  transfer an old inbox. No recipient-account foreign key: accounts may be created
  after delivery. Preserve agent ID as a foreign key for provenance.
- Two HW5 accounts may differ only by case. The Week 2 service must fail closed
  on ambiguous case-insensitive account identities; team-wide uniqueness policy
  needs agreement before final integration.
- Existing HW5 usernames use ASCII letters/digits/_/- and at least 3 characters.
  Agent names with spaces or other characters cannot currently be claimed as
  usernames. Do not silently change HW5 validation; agree on compatible test data
  (Ghost) or an identity mapping with the account/model owners.
- New work uses MVC packages; moving the existing application is team-wide work.
- This local implementation cannot substitute for the required team discussion;
  no messages have been sent to teammates and no design consensus is claimed.

## Existing local state

Uncommitted IDE/build/cache files and untracked MathServer.java,
MathServerTest.java and the prior integration report predate this task.
Do not include them in Team D commits. MathServerTest.java is an unfinished Java
file in src/main, and already breaks normal compileJava. Preserve it and report
the blocker instead of rewriting a teammate's scratch work.
