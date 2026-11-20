# Team D verification and limits

Verified against `NewDatabaseManagerRefactor` commit `18a534c` on 2026-11-20.
The working submission branch is NewDatabaseManagerRefactor; results for the
older main branch are not used as current branch failures.

## Reproducible commands

```sh
./gradlew test build unitTest integrationTest jacocoTestReport jacocoUnitTestReport
```

Executed with Java 26 and the repository wrapper, using cached dependencies with
`--offline --no-daemon`. Reports are generated under `build/reports`.

| Check | Measured result |
| --- | --- |
| Full test suite | 483 passed; zero failures, errors, or skips |
| Unit task | 389 passed |
| Integration task | 94 passed |
| Gradle build / packaged JAR | Passed |
| Complete-suite method coverage | 464 / 468 = 99.15% |
| Unit-task method coverage | 410 / 468 = 87.61% |
| Team D core services listed below | 61 / 61 methods in both tasks |

The Team D core-services measurement covers RecordEditor and AuditEvent,
SqliteRecordStore, RecordSearch and Match, RecordRelationships, RelatedRecordView,
and SessionManager and its Session/SessionSummary records. This is an explicitly
scoped result; it does not establish 100% coverage for every historical account
class, shared controller, teammate feature, or the entire application.
JaCoCo applies its normal generated-record filtering.

The complete suite exceeds the pasted >95% grading threshold, but does not meet
the current homework's explicit 100% target. If the TA measures only isolated
unit tests, the unit-task result does not meet >95%. Do not substitute combined
coverage for unit-only coverage without confirming the grading method.

Four complete-suite methods are missed: WebServer.serveStaticResource, its CSS
context lambda, PageHandler(AppController, SessionManager), and
ProgramControl.createMessageWebHandler. Some real SQLite tests are classified
under unitTest by the existing name filters; this is a suite-classification gap.

## Unit and integration evidence

Unit tests use Mockito for persistence, account lookups, and the JDBC transaction
protocol. Representative coverage includes RecordEditorTest,
RecordSearchTest, RecordRelationshipsTest, RecordPersistenceUnitTest,
SessionManagerTest, and SessionTimeoutTest.

The exact class-pair integration naming rule has existing examples:

- `src/test/java/org/view/AgencyInterfaceAuditLogManagerTest.java` integrates
  AgencyInterface with the real AuditLogManager and temporary SQLite.
- `src/test/java/org/view/AgencyInterfaceBriefManagerTest.java` integrates the
  terminal view and real BriefManager.

Additional Team D integration tests include AuditedWebRecordsIntegrationTest,
SqliteRecordStoreIntegrationTest, RecordRelationshipsIntegrationTest,
TerminalAccessIntegrationTest, and TerminalRecordIntegrityIntegrationTest.
The class-pair examples satisfy the naming evidence independently of the
additional `...IntegrationTest` names. Their current Gradle suite classification
still needs review; no tests were renamed by this documentation update.

## Acceptance behavior exercised

- Confirm all 196 supplied country names match the seed enum exactly and in order.
- Create/edit all five content types and preserve IDs, optional values, and
  multiline content; validate foreign references and reject non-admin writes.
- Commit scalar changes, mission junction changes, and one CREATE/UPDATE audit
  row atomically; roll back after forced audit failure.
- Persist username, action, content type, record identifier, and UTC timestamp.
- Show escaped related-record links; missing web details return 404 without READ.
- Search brief title/date/body, agent name/birth/death/notes, facility
  name/abbreviation, mission title, and country name.
- Refresh sessions after record/messaging requests, reject the exact 30-minute
  idle boundary, reload permissions, invalidate deleted accounts, list active
  tokens without refreshing them, and revoke a live token from the terminal.
- Preserve separate ciphered passwords, reject a replaced password, and keep
  password values out of SQLite account metadata.
- Reject malformed form encoding, foreign-origin POSTs, forged roles/tokens,
  invalid references, and unsafe static-resource paths.

## Additional stress and system evidence

The 2026-11-20 external audit at `184eed4` completed 307 assertion checks,
including 80 concurrent country writes on independent connections and 120
parallel completed login/read pairs. Unique IDs and matching audit rows were
verified. Recipient isolation, read flags, escaping, timeout, revocation,
permission changes, and audit rollback passed. That external harness is not
included in the Gradle/JaCoCo totals above.

The packaged-JAR country/session workflow passed at `184eed4`: first admin,
terminal login, -web-admin startup, HTTP login, country create/rename, facility
assignment, related links, persisted audit, terminal country search, live-token
revocation, and restart without duplicate seeds. The same workflow was rerun
at `18a534c` for this documentation update. Each other team member must still
personally acknowledge the assignment's individual system-test requirement.

## Confirmed unresolved cases

These are documented findings, not implemented fixes:

1. Terminal mission lookup drops a brief such as O'Brien after an unbound SQL
   lookup fails; the UI still reports successful mission creation.
2. A TSV with a valid first row and invalid second row leaves partial data while
   the terminal records an IMPORT count of zero; no per-record CREATE is logged.
3. BriefManager/AgentManager swallow read failures as empty results. Mission
   reads correctly fail explicitly, but that behavior is not universal.
4. A failed first-admin credential-file write leaves metadata with no working
   password, stranding normal bootstrap/login.
5. Case-sensitive accounts ghost/Ghost can both be created, but the messaging
   identity guard rejects both as ambiguous. No privacy bypass was observed.
6. An incomplete login POST blocks other requests until the connection closes.

The latest help stub still does not provide usable -h output, and terminal
record editing from the previous supplied assignment remains absent. Those
startup/view deliverables are assigned to the team in DELIVERABLES.md rather
than changed as part of this documentation-only work.

There is no claim that all possible bugs have been exhausted or that a grade
is guaranteed. Later commits require revalidation.
