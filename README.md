# Top Secret

UVA CS 3140 team application with a terminal interface and an authenticated HTML view with administrator record editing.

## Build and run

Use the checked-in Gradle wrapper with a compatible Java installation:

```sh
./gradlew test
./gradlew build
java -jar build/libs/TopSecret.jar
```

Run from the repository root so the application can find `data/key.txt` and its
SQLite database. The terminal prompts to create the first administrator account
when no accounts exist; otherwise it prompts for login. `--change-password`
opens password change after authentication.

```sh
java -jar build/libs/TopSecret.jar -web
```

The web view listens at http://localhost:3002/. Accounts must already exist.
The browser login uses session cookies. The server expires a token after 30
minutes of inactivity and refreshes activity on an authenticated request.
Administrators can create and edit records over the web. Each content change and
its audit entry commit together. Detail pages link to related agency records. Countries are listed at /countries
and terminal option 13. Facilities require a Country ID selected from that list.
Country names can be edited and appear dynamically at related facilities.
Existing facilities keep their data during migration and remain unassigned
until a user selects a country.

To administer the running server from the terminal, start both in one process:

```sh
java -jar build/libs/TopSecret.jar -web-admin
```

After terminal login, option 11 searches agency records. Administrators can use
option 12 to list active web tokens and revoke one. The server stops when this
terminal exits.

## Agent messaging

After terminal login, choose **9. Send an agent message**, select an existing agent
by ID, and write the message. A line containing `.` sends; `/cancel` cancels.
Choose **10. My messages** to view messages addressed to your username, matched
case-insensitively to the selected agent's name. No admin privileges are needed,
and admin status does not grant access to another user's inbox.

Messages may be sent before the recipient account exists. For example, a message
to agent Ghost becomes visible when the `ghost` account is created and logs in.
On startup, the app attempts to load the sample TSVs into empty tables.

## Source layout

- `org.runtime`: application entry point (`TopSecret`).
- `org.controller`: startup, authentication/session navigation and web controllers.
- `org.managers`: shared database and account/audit services.
- `org.model`: data models; `org.model.messaging` contains message storage/service.
- `org.view`: terminal and HTML views.
- `src/test/java/org`: unit and integration tests matching the active packages.

## Verification

```sh
./gradlew test unitTest integrationTest jacocoTestReport jacocoUnitTestReport build
```

The complete suite runs with `test`. The `unitTest` and `integrationTest`
tasks also run separately. See the [Team D verification notes](docs/team-d/VERIFICATION.md)
for the Homework 7 test results and remaining shared dependencies.

## Documentation

See the [documentation index](docs/README.md) for current component references.
[Team D documentation](docs/team-d/README.md) includes messaging, account/audit
schemas, the prioritized backlog, test evidence, and current design diagrams.
Older Team D plans and duplicate diagrams were removed; Git retains their history.
Shared component documentation remains subject to its owners' refactor review.
