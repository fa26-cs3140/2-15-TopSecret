# Top Secret

UVA CS 3140 team application with a terminal interface and a read-only HTML view.

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
The browser login uses session cookies. Web views are read-only, with plain HTML;
terminal menus own creation and editing. Agent messages remain terminal-only.

## Agent messaging

After terminal login, choose **8. Send an agent message**, select an existing agent
by ID, and write the message. A line containing `.` sends; `/cancel` cancels.
Choose **9. My messages** to view messages addressed to your username, matched
case-insensitively to the selected agent's name. No admin privileges are needed,
and admin status does not grant access to another user's inbox.

Messages may be sent before the recipient account exists. For example, a message
to agent Ghost becomes visible when the `ghost` account is created and logs in.
Sample agents must be created/imported; startup does not automatically load them.

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

The complete suite runs with `test`; `unitTest` and `integrationTest` also run
separately. Latest local verification: **322 tests, zero failures/errors/skips**;
build and coverage-report tasks pass. Empty MissionManager TODO tests were replaced
with substantive checks; none of the failing tests were disabled or excluded.

## Documentation

See the [documentation index](docs/README.md) for current component references.
[Team D documentation](docs/team-d/README.md) includes messaging, account/audit
schemas, regression fixes, test evidence, and current PDF class/ERD/wireframes.
Older Team D plans and duplicate diagrams were removed; Git retains their history.
Shared component documentation remains subject to its owners' refactor review.
