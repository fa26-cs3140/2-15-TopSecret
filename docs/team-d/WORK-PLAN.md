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

## Shared prerequisites and remaining submission work

1. **Team A country feature.** The shared `Country` class is unfinished.
   There is no country store, table, facility FK, or route. Once Team A supplies
   them, integrate countries into related-record links, terminal search, and
   the typed transactional edit path. The ERD marks that relationship as planned.
2. **Team C web messaging.** The shared branch still has terminal messaging.
   The Homework 7 web center and read indicators belong to Team C.
3. **Whole-project coverage.** Team D's record services, link view, and session
   manager have 100% JaCoCo method coverage in the isolated unit suite. The
   complete project still has uncovered inherited methods, including the
   unfinished Country class. See VERIFICATION.md for measured totals.
4. **Shared document cleanup.** Older Team A/C and legacy component documents
   remain in the repository. Their owners should reconcile them with the
   final countries, messaging, and view implementation before submission.
5. **Team review and system tests.** Review the integration PR and have each
   teammate run the final combined application before the deadline.

## Compatibility changes

Team B's forms and routes remain the normal editing entry points. AppController
now delegates complete writes to Team D's transactional store. Controller unit
tests inject a mock of that store and check the submitted audit metadata.
PageHandler adds related links and returns 404 for missing details. ProgramControl
shares a session manager between the terminal and web server in `-web-admin`
mode. The shared MissionManager implementation is retained.
