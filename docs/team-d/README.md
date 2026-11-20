# Team D model and access control

Team D's Homework 7 work provides audited content persistence, related-record
navigation, terminal search, and web session administration. The feature branch
is `team-d-hw7-week2`, based on the shared `NewDatabaseManagerRefactor` branch
and preserving the Week 1 commits.

- [Current design, ERD, and wireframes](DESIGN.md)
- [Completed work and remaining team dependencies](WORK-PLAN.md)
- [Test results and coverage](VERIFICATION.md)

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
live in the web server process and are cleared on restart.

## Record editing and audit

Team B's existing create/edit routes and form rendering call `AppController`.
It validates the complete submitted model and passes the authenticated actor
to `RecordEditor`. `SqliteRecordStore` writes all fields, mission junction
rows, and one CREATE or UPDATE audit row using one SQLite transaction.
SQL values use bound parameters and updates use the exact numeric record ID.
A failure rolls back both the content and audit writes. Missing update targets
create no audit entry. Multiline text and apostrophes persist literally.

Audit entries retain username, action, content type, content identifier, and
timestamp. CREATE uses the generated record ID. Existing READ auditing is
preserved, and missing detail pages return 404 without a READ event.

## Relationships and search

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
search matches country names. Terminal option 13 opens Countries.

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
Team C owns Homework 7's web messaging center and read indicators. The content
edit/search services operate on agency records. The existing messaging privacy
tests remain active.
