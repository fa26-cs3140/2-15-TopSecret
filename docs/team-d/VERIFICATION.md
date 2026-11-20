# Final Team D verification

Checked on 2026-11-20 against shared snapshot `6af24d1` plus the final-unit-coverage test commits. The final documentation
commit follows that source snapshot. Main is not the current submission branch.

## Reproducible commands and measured results

```sh
./gradlew test build unitTest integrationTest jacocoTestReport jacocoUnitTestReport
```

Executed with Java 26 and the checked-in Gradle wrapper. Results:

| Check | Result |
| --- | --- |
| Complete suite | 517 passed; 0 failures, errors, or skipped tests |
| Separate unit task | 413 passed |
| Separate integration task | 104 passed |
| Build and executable JAR | Passed |
| Complete-suite method coverage | 475 / 475 = 100% |
| Unit-task method coverage | 475 / 475 = 100% |

Reports: build/reports/tests/test/index.html,
build/reports/jacoco/test/html/index.html, and
build/reports/jacoco/jacocoUnitTestReport/html/index.html.

Both the complete-suite and unit-task reports now cover 475/475 methods (100%).
The new tests isolate HTTP exchanges, controller/service dependencies, and JDBC
using Mockito. No production methods or tests were excluded or reclassified to
raise coverage. Existing legacy filename filters still classify some real SQLite
class-pair tests under unitTest; the new unit tests do not open a server or SQLite.
Method coverage is not branch coverage or a guarantee against every possible bug.

Mockito isolates service, account, persistence, and JDBC boundaries in tests
including RecordEditorTest, RecordPersistenceUnitTest, RecordSearchTest,
RecordRelationshipsTest, SessionManagerTest, and SessionTimeoutTest.
Class-pair integration examples include AgencyInterfaceAuditLogManagerTest,
AgencyInterfaceBriefManagerTest, and AgencyInterfaceMissionManagerTest.
Additional HTTP/SQLite tests use the IntegrationTest suffix and run separately.

## Functionality and regression evidence

- All five web content types: create/edit, exact IDs, optional values, related
  links, country assignment/rename, HTML escaping, administrator-only writes.
- Atomic scalar, mission-junction, and audit writes; forced audit failures roll
  back content. Events retain actor, action, content type, identifier, and UTC time.
- Complete-file terminal imports: invalid late row, missing foreign key, and
  rejected aggregate audit event leave no partial rows or CREATE events.
  Successful imports persist each CREATE and the correct IMPORT count.
- Apostrophes in terminal mission brief/agent selection preserve related IDs;
  SQL-looking search values remain data and unknown columns are rejected.
- Failed brief/agent storage reads fail explicitly rather than masquerading as
  empty results. Existing mission read-error tests also pass.
- First-account credential-write failure leaves no metadata and permits retry;
  metadata failure attempts credential cleanup. Filesystem and SQLite are not
  one distributed transaction, so a process crash is outside that guarantee.
- Case-equivalent account insertion is rejected even across two store instances.
  A deliberately seeded legacy ambiguous-account fixture still fails closed in
  messaging; no privacy guard was removed.
- Passwords remain in separate ciphered files; password replacement invalidates
  the old value. SQLite account metadata has no password column.
- Search covers agency text fields. Sessions refresh activity, expire at the
  exact 30-minute idle boundary, reload roles, reject deleted accounts, and support
  administrator terminal listing/revocation without refreshing listed sessions.
- One unfinished login body does not block another visitor. Forms exceeding
  64 KiB are rejected without login. The bounded pool can still be exhausted by
  enough stalled clients; no unlimited-load guarantee is claimed.

## Packaged system and adversarial checks

The rebuilt executable JAR passed a disposable-data workflow: first admin,
terminal login, -web-admin, HTTP login, country creation/rename, facility
assignment, both related links, persisted audit, terminal country search,
live-token revocation, restart with no duplicate country seeds.
The packaged -h option also passed, printing help and exiting without login.

The external real-HTTP/SQLite harness was recompiled against the final JAR and
passed 307 assertion checks, including 80 concurrent country writes and 120
parallel completed login/read pairs. It exercises malformed inputs, forged roles,
foreign-origin POSTs, recipient privacy including no admin override, persistent
read indicators, escaping, exact timeout, revocation, and audit rollback.
These checks are additional evidence, not part of the 517 Gradle test count.

Each teammate must personally confirm the assignment's individual system test.
See DELIVERABLES.md for remaining owner-specific issues. No test run can certify
that every possible bug has been found or guarantee a grade.
