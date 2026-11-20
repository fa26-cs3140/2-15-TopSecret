# Team D model and access control

Team D's Homework 7 work provides audited content persistence, related-record
navigation, terminal search, and web session administration. The current integrated
branch is `NewDatabaseManagerRefactor`; the Week 1
and Week 2 feature commits remain in its history. Documentation was checked
against `18a534c` on 2026-11-20. Main is not the team's current working branch.

- [Current design, ERD, and wireframes](DESIGN.md)
- [Completed work and remaining team dependencies](WORK-PLAN.md)
- [Test results, coverage, and confirmed failure cases](VERIFICATION.md)
- [Grading checklist and teammate handoff](DELIVERABLES.md)
- [Team D UML PDF](TeamD-ClassDiagram.pdf)
- [Entity-only ERD PDF](TeamD-ERD.pdf)
- [Team D wireframe PDF](TeamD-Wireframes.pdf)
- [Shared team roster](../../teams.txt)

Team D: Vrisag Desinani; UVA computing ID `fae9xd`; GitHub `VrisagDesinani`.
The PDFs cover Team D's responsibilities and integration boundaries. They do not
replace the shared whole-system UML or Team C's messaging screen wireframes.

## Running the application

Build with `./gradlew build`, then run from the repository root.

```sh
java -jar build/libs/TopSecret.jar
java -jar build/libs/TopSecret.jar -web
java -jar build/libs/TopSecret.jar -web-admin
```

The default mode opens the authenticated terminal. `-web` starts the web
interface on port 3002 after accounts exist. `-web-admin` authenticates the
terminal user and starts the web server in the same process. The server stops
when that terminal exits. Accounts and passwords are managed through the
existing terminal workflow.

Terminal option 11 searches content text fields. Option 12 in `-web-admin`
lists current web sessions for administrators, including username, full token,
and last activity in UTC. Enter a token to revoke it, `r` to refresh the list,
or an empty line to return. The next request with a revoked token goes to
login. The list operation preserves the recorded last-activity times.

A valid web request refreshes the session's activity. Thirty minutes without
a request expires the token, including at the exact deadline. Each production
session lookup reloads current account permissions. A deleted account loses
access and a demoted administrator loses permission to edit. Session tokens
live in the web server process and are cleared on restart. Terminal admin checks
also reload current permissions. Cross-origin browser POSTs are rejected and
session cookies explicitly use SameSite=Lax; ordinary same-origin forms and
clients without browser origin metadata remain supported.

## Record editing and audit

Team B's existing create/edit routes and form rendering call `AppController`.
Terminal content creation also calls that controller, refreshing the actor's
permissions immediately before saving.
It validates the complete submitted model and passes the authenticated actor
to `RecordEditor`. `SqliteRecordStore` writes all fields, mission junction
rows, and one CREATE or UPDATE audit row using one SQLite transaction.
SQL values use bound parameters and updates use the exact numeric record ID.
A failure rolls back both the content and audit writes. Missing update targets
create no audit entry. Multiline text and apostrophes persist literally.

Audit entries retain username, action, content type, content identifier, and
timestamp in UTC. Historical entries without offsets are preserved unchanged.
CREATE uses the generated record ID. Existing READ auditing is
preserved, and missing detail pages return 404 without a READ event.

## Relationships and search

Mission and assignment reads fail explicitly on storage errors, so an outage
cannot be presented as an empty edit form. The web server returns 503 and
preserves the saved assignments.

`RecordRelationships` supplies related records through the existing managers.
`RelatedRecordView` builds internal links from record types and IDs and escapes
the display labels.

- Facilities link to their country, agents, and missions.
- Countries link to their assigned facilities.
- Agents link to their facility and assigned missions.
- Missions link to their facility, briefs, and agents.
- Briefs link to assigned missions.

`RecordSearch` performs literal, case-insensitive substring matching across
brief title/date/text, agent name/birth/death/notes, facility name/abbreviation,
mission title, and country name. A record appears once even if several fields match. Empty
queries produce no matches. Search returns content references to the terminal.

Countries are seeded from Team A's supplied enum and can be created or renamed
through the existing web forms. The facility form requires a Country ID from
the Countries list. Related links connect countries and facilities, and terminal
search matches country names. Terminal option 13 opens Countries. Terminal facility creation accepts a
country name or numeric ID, and Exit cancels country selection.

Legacy facilities retain their IDs and other values during migration. The
three-argument Facility constructor and two-column sample TSVs remain compatible.
Their country is unassigned until a user selects one. Reinitialization preserves
edited country names and prevents duplicate seed rows.

## Existing accounts and messaging

Passwords remain in separate `credentials-<hex username>.cip` files. SQLite
stores username and admin metadata. Username and password rules are enforced
by the existing account manager. Tests verify separate passwords, password
replacement, and the account-table columns.

Terminal option 9 sends a message and option 10 opens the current user's inbox.
Team C's Homework 7 web messaging center and read indicators are integrated.
The shared navigation opens `/messages`; Team C maintains its inbox, compose,
and message-detail behavior. The same session manager authenticates messaging
and record requests. The content edit/search services operate on agency records.
Case-duplicate accounts currently trigger an ambiguous-identity error in messaging;
this is an open account/messaging integration issue, not a privacy override.

## Tests and remaining work

Run `./gradlew test build` for the complete suite/build, and use
`./gradlew unitTest integrationTest jacocoTestReport jacocoUnitTestReport` for
separate tasks and reports. Current combined method coverage is 464/468 (99.15%);
it exceeds the pasted >95% threshold but misses this homework's 100% target.
The explicitly listed core services have 61/61 method coverage. Consult
VERIFICATION.md for exact scope; some legacy integration tests run in unitTest.

The documentation does not certify that the application is bug-free. Open Team
D/shared failures include partial-import audit counts, quoted terminal mission
lookups, silent brief/agent read errors, failed credential-file bootstrap, and
case-duplicate messaging identities. Startup help, shared screen designs, and
the current whole-system UML need their owners' attention. DELIVERABLES.md
contains the actionable handoff and distinguishes grading gaps from optional
hardening. No production code was changed for this documentation update.
