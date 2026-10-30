# Team D Week 1 - start here

Branch: team-d-agent-messaging. This is the test-first Week 1 milestone, not a
finished agent-messaging feature. The branch remains separate from main.

Read WORK-PLAN.md, TEST-PLAN.md, DESIGN.md and VERIFICATION.md in this folder.
The three adjacent PDFs document classes, schema and proposed interactions.

Executable tests added: MessageTest (4), MessageControllerTest (6),
MessageSchemaIntegrationTest (4). Eight additional scenarios in the abstract
MessagingAcceptanceContract are drafts waiting for a real Week 2 fixture.

For Week 2, implement the session-bound MessageService and MessageStore,
implement MessageView for the terminal, bind the controller after successful
login, resolve identity-policy questions with the team and make the acceptance
contract runnable. Do not merge until the complete feature is tested together.

Run normal ./gradlew test and ./gradlew build on a clean checkout. The current
local checkout also has an unrelated unfinished untracked MathServerTest.java
in production sources; it already breaks compilation and is not in this branch.
See VERIFICATION.md for the clean-snapshot build and terminal-JAR smoke results.
