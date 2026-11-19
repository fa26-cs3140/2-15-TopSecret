# Homework 7 verification

## Automated results

Executed with Java 26 and the repository's Gradle wrapper:

```sh
./gradlew test build unitTest integrationTest jacocoTestReport jacocoUnitTestReport
```

| Check | Result |
| --- | --- |
| Full test suite | 412 passed, zero failures, zero errors, zero skips |
| Separate unit task | 348 passed |
| Separate integration task | 64 passed |
| Gradle build and packaged JAR | Passed |
| Project method coverage, complete suite | 388 / 411 methods, 94.4% |
| Project method coverage, unit task | 351 / 411 methods, 85.4% |
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

The uncovered project methods are in TopSecret, MissionManager, FileHandler,
Country, SearchResult, AgencyInterface, HtmlPage, and DebugHelper. The complete
project has further work to reach the assignment's 100% method target.

## Acceptance cases exercised

- Create and edit briefs, agents, facilities, and missions over HTTP.
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

Country links, country search, and the country's own editing path require Team
A's unfinished feature. Web messaging and read flags require Team C's feature.
Those shared dependencies are listed in the PR and WORK-PLAN.md.

## Final formatting and documentation audit

Expanded the inherited terminal message renderer, message getters, and service
formatting. The full suite and build passed again after those changes. Replaced
superseded class notes and older homework diagrams with the maintained class
PDF, ERD, and wireframes. The current model overview and team roles now match
the code and Homework 7 responsibilities.
