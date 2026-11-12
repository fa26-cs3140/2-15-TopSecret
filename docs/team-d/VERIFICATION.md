# Homework 7 verification

## Week 1 automated checks

Run from the repository root:

```sh
./gradlew test build unitTest integrationTest jacocoTestReport
```

| Check | Result |
| --- | --- |
| Full test suite | 347 passed; zero failures or skips |
| Separate unit task | 304 passed |
| Separate integration task | 43 passed |
| Gradle build | Passed |
| Project method coverage | 313 of 339 methods, 92.3% |
| New Team D code and changed session manager | All JaCoCo-counted methods exercised by the combined suite |

The existing Gradle task filters determine which suites run separately.
The new SQLite relationship and HTTP tests run with the integration task.
JaCoCo counts a method as covered when it executes at least one instruction;
this does not establish coverage of every path.

## Week 1 packaged application check

The built JAR passed a smoke test in a temporary data directory: first-account
creation, a per-user ciphered credential file, the corrected messaging menu
labels, browser login, authenticated navigation, record listing, and logout.
Timeout and token revocation were exercised by the automated HTTP tests using
a controlled clock.

## Week 2 acceptance and system test

Run each scenario against a fresh database, and rerun against a database
created before the changes. Each teammate should run the packaged app ahead
of the final deadline.

| Scenario | Pass condition |
| --- | --- |
| Admin creates each supported record from an empty form | New values persist, and exactly one CREATE entry has the actor, type, new ID, and timestamp |
| Admin edits each supported record from a prefilled form | Saved fields and associations survive restart; one UPDATE audit event identifies the same record |
| Invalid edit or forced audit failure | No record or relationship changes commit, no audit event persists, and the form reports the problem |
| Unknown ID or standard user's edit request | No changes and no CREATE/UPDATE audit rows |
| Apostrophes and multiline text in an edit | Content is stored literally; HTML output is escaped |
| Related records | Every displayed link opens the correct record; changing a country updates the name shown at its facilities |
| Terminal search | Match every content text field, including country data; punctuation is literal and empty queries return no matches |
| Web timeout | A request before minute 30 refreshes activity; after 30 idle minutes the next request goes to login |
| Terminal session administration | Admin sees username, token, and last activity; revoking one token signs out only that browser on its next request; standard users are denied |
| Existing account and messaging paths | Separate passwords still work, previous passwords fail after a change, and inboxes stay isolated |

The transactional write, country, clickable relationship, and terminal menu
scenarios are Week 2 acceptance checks. The current tests exercise the Week 1
contracts, existing routes, and implemented idle timeout.
