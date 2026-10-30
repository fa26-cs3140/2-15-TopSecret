# Team D Week 1 verification

Verified 29 October 2026. Work is on team-d-agent-messaging, based on main 09008ca.
There are four focused commits (plan; tested model/contracts; MVC/schema tests
and acceptance drafts; diagrams/handoff). No merge or push was performed.

## Clean committed-source verification

A `git archive HEAD` snapshot of the third commit (6e0cd8f) was extracted outside
the checkout. This contains the actual committed source and excludes unrelated
untracked user scratch files. The final fourth commit adds documentation only.
No exclusions, source edits or verification-only Gradle init script were used in
this clean-snapshot run.

Command:

```
./gradlew test unitTest integrationTest jacocoTestReport jacocoUnitTestReport build
```

- test: PASS, 226 tests, 0 failures, 0 errors, 0 skipped.
- unitTest: PASS, 181 tests, 0 failures, 0 errors, 0 skipped.
- integrationTest: PASS, 45 tests, 0 failures, 0 errors, 0 skipped.
- build: PASS, including JAR and distributions.
- Both coverage-report tasks: PASS.
- 14 new executable tests: 4 model, 6 mocked controller, 4 real SQLite schema tests.
- 8 abstract acceptance scenarios compile but do not execute and are not included
  in the passing-test count. Privacy/delivery is a contract, not live functionality.

JaCoCo unit and full-suite reports each show Message at 7/7 methods, 20/20 lines,
18/18 branches; MessageController at 4/4 methods, 15/15 lines, 4/4 branches.
This is 100% coverage of the two concrete Week 1 classes only. Interfaces contain
no implementation; no percentage is claimed for future service/store/view code.

## Packaged JAR smoke test

Launched the clean-snapshot fat JAR with `java -jar`, from a temporary directory
containing a copy of mission_briefs.tsv. It displayed Top Secret Mission Menu
and exited normally after input 4. Confirmed that Message, MessageService,
MessageController, MessageView and schema.sql are packaged in the JAR.
This checks the existing terminal entry point and packaging, not messaging,
account creation/login, seeded agent navigation or the future web service.
Those end-to-end flows remain part of Week 2 team integration.

## Current dirty-checkout blocker

Before adding code, ordinary ./gradlew test failed in compileJava on the
pre-existing untracked src/main/java/org/example/MathServerTest.java (unfinished
testServer statement and catch clause, lines 10-11). That file is untouched and
is not committed on the Team D branch. The original dirty checkout therefore
still needs that scratch file repaired or moved out of production sources before
normal Gradle compilation can pass. No permanent build exclusion was added.

A prior diagnostic run excluded exactly that scratch file via an external init
script and also passed all 226 tests/build. The independent clean-snapshot run
above establishes that the actual committed branch builds without exclusions.

Existing DatabaseManager tests emit SQLITE_DONE diagnostics, and Gradle/Mockito/
Java emit deprecation, native-access and agent-loading warnings. They do not fail
the suites. These unrelated implementations were not modified.

## Preservation and review

No previous Team D source was removed: HW4 UI and HW5 authentication/audit remain
relevant. No existing teammate source, build configuration or entry point was
changed. Initial IDE/build/cache modifications and unrelated untracked files are
preserved outside the new commits. Generated repository build/cache contents
from this run are restored to their captured pre-task state after saving evidence.

Artifacts were visually inspected after rendering. The schema diagram clearly
labels agent_messages as proposed and not yet applied to the live application.
Shared HTML wireframes are proposals for the view owner, not an implemented site.

## Week 2 work deliberately left open

Authenticated service implementation, SQLite message repository, real terminal
prompts/menus, account/agent identity-policy agreement, concrete acceptance
fixture, application-wide MVC/startup integration, full login/test-data/JAR
verification with all team members and final review/merge. No automatic merge,
push, teammate message or Gradescope submission has occurred.
