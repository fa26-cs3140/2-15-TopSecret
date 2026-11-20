# Homework 7 verification

## Automated results

Executed with Java 26 and the repository's Gradle wrapper:

```sh
./gradlew test build unitTest integrationTest jacocoTestReport jacocoUnitTestReport
```

| Check | Result |
| --- | --- |
| Full test suite | 441 passed, zero failures, zero errors, zero skips |
| Separate unit task | 366 passed |
| Separate integration task | 75 passed |
| Gradle build and packaged JAR | Passed |
| Project method coverage, complete suite | 442 / 442 methods, 100% |
| Project method coverage, unit task | 398 / 442 methods, 90.0% |
| New Team D services, link view, and session manager | 100% method coverage in both the isolated unit task and complete suite |

The Team D coverage statement includes RecordEditor and AuditEvent,
SqliteRecordStore, RecordSearch, RecordRelationships, RelatedRecordView,
and SessionManager. JaCoCo-generated record filtering applies. Method coverage
measures whether a method executes; the acceptance cases below exercise
failure paths and complete workflows as well.

Unit tests use Mockito for record stores, account services, and the JDBC
transaction protocol. Integration tests use temporary SQLite databases and
real HTTP servers. The existing Gradle task filters determine the separate
suite membership. Tests use the normal application constructors and routes.

The complete-suite report has zero missed methods. The additional regression
checks exercise startup delegation and failure reporting, file-directory
reconfiguration, utility accessors, terminal role changes, record imports, and
mission creation. An unused unimplemented audit-table stub was removed after
confirming that the application has no callers.

## Acceptance cases exercised

- Create and edit briefs, agents, facilities, missions, and countries over HTTP.
- Preserve generated IDs, optional nulls, apostrophes, and multiline content.
- Replace mission assignments, including empty lists.
- Persist username, action, content type, record ID, and timestamp in audit.
- Roll back scalar values, assignments, and creation after a forced audit failure.
- Reject standard-user POSTs and revoke a demoted administrator's edit access.
- Render related links in every current record direction and escape labels.
- Return 404 for missing details without recording a READ event.
- Search all current record text fields through the terminal menu.
- Refresh active sessions beyond their original lifetime and expire them at
  the exact 30-minute idle boundary.
- List and revoke a live token through the authenticated terminal.
- Preserve separate ciphered password files and invalidate an old password
  after replacement.

## Packaged application system test

Ran the built JAR against a temporary copy of the data directory. Created an
administrator through the terminal, restarted in `-web-admin` mode, logged in
over HTTP, created/read/edited a brief, and inspected the persisted audit
fields. Listed and revoked that live token through terminal option 12. Its
next HTTP request redirected to login. Terminal option 11 found the edited
body, and terminal exit stopped the server cleanly. The process exited with 0.

## Country integration checks

The country feature is integrated from Team A's shared commits. Tests cover
initial seeding, repeated initialization, migration of an existing facility
table, preservation of IDs and data, newly created country IDs beyond the
seed count, country renaming, country/facility links, country search,
reassignment, invalid IDs, authorization, and rollback after an audit failure.

The packaged JAR also passed a country workflow in a temporary data directory.
It created and renamed a country over HTTP, assigned a facility, followed both
related links, found the renamed country from terminal search, inspected the
persisted audit entries, and revoked the live token from the terminal. A restart
preserved the edited country name and left the country count unchanged.

Team C's web messaging and Team A's final styling/help remain separate shared
submission work. Country integration and the whole-project method target are
complete on this branch.

## Final formatting and documentation audit

Expanded the inherited terminal message renderer, message getters, and service
formatting. The full suite and build passed again after those changes. Replaced
superseded class notes and older homework diagrams with the maintained class
PDF, ERD, and wireframes. The current model overview and team roles now match
the code and Homework 7 responsibilities.

## Adversarial testing

The real HTTP/SQLite and terminal tests reproduced five failures before their
fixes. Malformed form encoding now returns 400, encoded slash-only paths return
404, and a storage read outage returns 503 with a generic response. The server
continues accepting requests after these failures. Terminal mission navigation
accepts existing facilities with IDs other than 1, and an unknown facility name
reports an error without crashing or saving a mission.

Additional tests pass hostile SQL/HTML strings through create and read routes,
attempt role spoofing in form fields, use forged/missing/deleted-account cookies,
and create countries concurrently. Values stay literal, labels are escaped,
unauthorized changes are rejected, and concurrent IDs and audit entries remain
consistent. The full suite passes with zero failed, errored, or skipped tests.
