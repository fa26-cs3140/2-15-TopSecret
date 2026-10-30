# Team D messaging - Week 1 test plan

## Executable Week 1 tests

MessageTest: constructor and every getter; reject negative ID, missing/blank
sender, nonpositive recipient agent ID, missing/blank recipient name/body, and
missing timestamp. ID zero represents an unsaved message. Preserve multiline
body and original agent display name; immutable objects have no setters.

MessageControllerTest (Mockito): null dependency validation; agent listing,
sending and inbox rendering; delegate without taking a username from the UI;
show success only after service returns; report validation failure without
claiming delivery. Keep the authenticated service as the privacy boundary.

MessageSchemaIntegrationTest: execute shipped DDL against temporary SQLite;
initialize twice safely; all fields and generated ID persist; nonexistent sender
or agent rejected; no recipient account needed; case-insensitive recipient query;
blank bodies rejected; existing users/agents untouched; index exists.

## Draft Week 2 acceptance contract

MessagingAcceptanceContract is an abstract JUnit fixture contract. It compiles
now but is not runnable until a concrete Week 2 fixture extends it. Do not report
these scenarios as verified behavior or include them in passing-test counts.

1. Send to Ghost before Ghost has a user account; create account later and read.
2. Deliver to a username matching the agent name with different case.
3. Ordinary users can read their own inbox, not another user's; admins also have
   no override to read another user's messages.
4. Reject invalid/nonexistent agent IDs and blank/null messages.
5. Persist sender from the authenticated session, body, target and timestamp.
6. Preserve inbox ordering and contents through service/database reconstruction.
7. Reject ambiguous case-insensitive account identities without revealing messages.
8. Reject unauthenticated sessions at service creation.

The future concrete fixture must seed users/agents explicitly, use real SQLite
and authentication, and extend the contract in an *IntegrationTest class so the
existing Gradle integrationTest task discovers it.

## Coverage and build interpretation

Use ./gradlew test, unitTest, integrationTest, jacocoTestReport,
jacocoUnitTestReport and build. Check the new Message and MessageController
classes separately from legacy code; interfaces have no executable method bodies.
A passing schema test verifies the DDL, not a future repository/service.
The immutable model intentionally has no setters to test.

Normal baseline Gradle compilation currently fails on the pre-existing,
untracked src/main/java/org/example/MathServerTest.java. A verification-only
Gradle init script may exclude exactly that file to test the committed tree plus
Week 1 additions. This is a diagnostic workaround, not a committed build change
or proof that the original dirty checkout builds unmodified.
