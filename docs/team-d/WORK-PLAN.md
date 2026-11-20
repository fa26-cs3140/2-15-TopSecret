# Homework 7 model and session work

The Week 1 commits contain the design and test contracts. The Week 2 branch
integrates the latest shared web edit workflow and completes Team D's services
for the available content types.

## Completed backlog

| Item | Implementation and validation |
| --- | --- |
| Terminal messaging options | Corrected Send/Inbox labels and repaired regression tests that entered the password menu |
| Content edits and audit | Typed model validation, exact-ID prepared writes, one transaction for content, mission relations, and CREATE/UPDATE audit |
| Failed writes | SQLite trigger tests force audit failure and verify rollback of scalar fields, associations, and creation |
| Related records | Facility, agent, mission, and brief detail pages display escaped links to related records |
| Missing record | Web details return 404 and leave audit history unchanged |
| Search | Option 11 searches all text fields of the current content models, with literal punctuation and case-insensitive matching |
| Session idle timeout | Active requests refresh activity; sessions expire at 30 minutes of inactivity |
| Session permissions | Production session lookup reloads account permissions and rejects deleted accounts |
| Session administration | Option 12 in the authenticated `-web-admin` terminal lists and revokes the live server's tokens |
| Tests and system check | Separate unit/integration tasks, full Gradle build, HTTP/SQLite acceptance tests, packaged-JAR system test |
| Documentation | Updated diagrams and current instructions; regenerated the shared class PDF from compiled classes |

## Team integration status

1. **Country integration completed.** Incorporated Team A's country model,
   repaired initialization and lookup problems, migrated existing facilities,
   and connected country edits, relationships, and search. Regression tests
   verify seeded IDs, renames, restart behavior, and legacy data preservation.
2. **Team C web messaging.** The shared branch still has terminal messaging.
   The Homework 7 web center and read indicators belong to Team C.
3. **Whole-project method coverage completed.** The complete test suite exercises
   all 443 measured methods. Added checks cover startup argument handling,
   directory changes, search-result accessors, diagnostics, and terminal
   account/import/mission workflows. The unused audit-table stub had no
   callers and has been removed.
4. **Final shared diagrams.** Superseded class notes and old diagrams have been
   removed in favor of the current class PDF, ERD, and wireframes. Update those
   current diagrams again when the web messaging feature arrives.
5. **Team review and system tests.** Review the integration PR and have each
   teammate run the final combined application before the deadline.

## Compatibility changes

Team B's forms and routes remain the normal editing entry points. AppController
now delegates complete writes to Team D's transactional store. Controller unit
tests inject a mock of that store and check the submitted audit metadata.
PageHandler adds related links and returns 404 for missing details. ProgramControl
shares a session manager between the terminal and web server in `-web-admin`
mode. MissionManager retains its data model and SQL layout; read failures now
propagate instead of being mistaken for an empty record or assignment list.

## Country compatibility corrections

The country push removed the previous Facility constructor and manager overload.
They remain available for existing callers. The new four-argument constructor
validates positive country IDs. SQLite enforces the referenced country's existence.

Country initialization uses explicit seed IDs with INSERT OR IGNORE, and adds the
new facility column to existing databases. Country lookup includes both ID and
name and works after renaming. Names are editable and newly created countries
can receive IDs beyond the original enum. Facility lookup binds input values.

The current web forms require Country ID for new or edited facilities. Legacy
unassigned records are preserved without assigning an invented country.

## Adversarial review

Fixed malformed form/encoded-path handling, controlled storage-read errors,
the hard-coded facility ID in terminal mission navigation, and the crash on
an unknown facility name. Regression tests exercise each failure through
the HTTP server or terminal menu. SQL/HTML injection strings, forged roles
and tokens, deleted accounts, and concurrent writes are also exercised.

The follow-up review also fixed stale terminal permissions, non-transactional
terminal CREATE auditing, misleading empty mission forms after read errors,
and mixed audit time zones. Terminal creation now uses the same controller and
transactional store as web creation. Browser POST origins are checked and the
session cookie explicitly uses SameSite=Lax. Regression tests and the packaged
system workflow pass with these changes.
