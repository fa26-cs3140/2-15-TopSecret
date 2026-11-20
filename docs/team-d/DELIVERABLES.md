# Working-branch deliverables review

Checked on 2026-11-20 against NewDatabaseManagerRefactor `18a534c`, using the
current assignment and the additional grading comments supplied by the user.
This document concerns that working branch, not older main-branch failures.
Only Team D documentation and the explicitly supplied shared roster were updated.
No production code, tests, teammate documentation, or shared global UML was edited.

## Rubric matrix

| Deliverable | Current evidence | Status / owner action |
| --- | --- | --- |
| README describes current solution/run/schema | Team D README + DESIGN schema notes updated | Team D current; root README owner should add current web messaging/read-state workflow and link current diagrams |
| teams.txt with all names, computing IDs, GitHub usernames | Root teams.txt, IDs supplied by Vrisag | Updated; moved existing docs/team.txt with history retained |
| System UML PDF matches actual system | Shared docs/HW7-ClassDiagram.pdf predates messaging | Shared/Team C update needed; Team D scoped PDF is current but is not a whole-system replacement |
| ERD entities/relationships without columns | Team D ERD PDF and Mermaid ERD now omit attributes | Team D corrected; physical DDL is separate in DESIGN.md |
| Current wireframes | Team D related-data/search/session PDF | Team D current; Team C still needs messaging inbox, compose, message-detail/read-state wireframes |
| >95% method coverage including boilerplate | Complete suite 464/468 = 99.15%; unit task 410/468 = 87.61% | Combined passes >95%, but unit-only does not; current assignment explicitly requires 100%, which is unmet |
| Unit stubs/Mockito and integration tests | Tests pass; mocks used at persistence/account/JDBC boundaries | Present; Gradle unit filters still include some SQLite integration tests |
| Integration ClassAClassBTest naming | AgencyInterfaceAuditLogManagerTest.java integrates those real classes | Present; other IntegrationTest names do not negate the class-pair example |
| Web create/edit and admin restriction | Full HTTP/SQLite acceptance suite | Implemented and normal/adversarial cases pass |
| Country seed/display/edit/reassignment | Country migration and HTTP/system checks | Implemented; all 196 enum names match supplied data/countries.txt exactly and in order |
| Related-record navigation | RecordRelationships and RelatedRecordView | Implemented for current stored agency relationships; quoted terminal selection still has a bug |
| Audit all new changes | Transactional CREATE/UPDATE + READ and session-related tests | Normal record paths pass; partial TSV import has an audit gap |
| Web messaging center | Team C code integrated; recipient/read-state tests pass | Core behavior passes; case-duplicate account creation can disable messaging |
| CSS/menu/logout, no JS/images | Shared template + static CSS | Integrated; audit text contrast corrected in 184eed4; duplicate Facilities link remains |
| -h help | Stub added in 6757077 | Incomplete; see Team A handoff below |
| Frequent small commits | Incremental design, implementation, test, and repair commits | Evidence present; no exact grading threshold was supplied |
| Every member runs system test | Team D packaged workflow rerun | Each teammate must personally confirm their final JAR/system test |
| Previous terminal editing requirement | No edit option in AgencyInterface | Shared view/controller carry-over gap; verify whether course staff waived it |

The pasted system-UML rubric appeared twice; it is one deliverable unless the
actual Gradescope assignment lists two separately graded diagrams. The current
homework's 100% requirement is retained instead of silently replacing it with an
older >95% threshold.

## Scope and verified results

483 full-suite tests passed, 389 unit-task tests passed, 94 integration-task tests
passed, and the build passed. Four methods remain untested in the full suite:
WebServer.serveStaticResource, its CSS-route lambda, the two-argument PageHandler
constructor, and ProgramControl.createMessageWebHandler. This is method coverage,
not branch coverage and not a guarantee of all behavior.

The separate 2026-11-20 stress harness at 184eed4 passed 307 assertion checks,
including 80 concurrent SQLite country writes and 120 parallel completed
login/read pairs. Password isolation, recipient privacy, timeout, revocation,
role spoofing, literal content, and atomic audit rollback were exercised.
Those external checks are not counted in the Gradle coverage figures.

## Team D / shared defects still open

These are not fixed by rewriting documentation:

- Terminal mission selection of a brief such as O'Brien produces a SQL error,
  saves the mission without that brief, and reports success. Agent lookups use
  the same unbound-query pattern.
- A valid TSV row followed by an invalid date persists the first row while
  logging IMPORT count zero. The imported row has no CREATE audit entry.
- BriefManager and AgentManager report storage read failures as empty lists.
- Failed first-admin credential-file writes leave account metadata without a
  working password and prevent normal bootstrap recovery.
- Account creation allows ghost/Ghost, but the case-insensitive messaging
  identity guard rejects both as ambiguous. This is availability failure,
  not a demonstrated cross-user message disclosure.
- One incomplete login POST blocked other requests until its connection closed.
  This is a shared server-hardening finding, not an explicit load threshold in
  the assignment.

## Handoff to teammates / shared-document owners

### Team A - Zaid

- Complete -h. Current run checks `args[0] == "-h"` rather than string equality,
  so actual CLI arguments do not reliably match. It reads `data/help.txt` through
  FileHandler, which already prefixes `data`; help.txt itself is empty.
  At 18a534c, a packaged JAR with -h and no input still prompts for username and
  prints NoSuchElementException. Do not count the stub as working help.
- Remove the duplicate Facilities navigation entry in the shared template.
- Audit text color is now corrected; the earlier contrast finding is resolved.
- Country seed verification passed: all 196 names in data/countries.txt match
  the current enum exactly and in order. No country-list mismatch was found.

### Team B - Dillon

- Root README should describe the current editing/navigation workflow and link
  the current system diagrams; Team D did not rewrite the root/shared README.
- Review the missing terminal record-edit path from the previous assignment.
- Coordinate the partial-import and quoted mission-selection failures with the
  model owner; these span terminal prompts and manager persistence.
- Coordinate any slow/incomplete request hardening in WebServer.

### Team C - Mike

- Refresh the shared whole-system UML with MessageWebHandler, MessageHtmlView,
  Message.isRead/read constructor, MessageService.readMessage, MessageStore
  markAsRead, and the new web/startup relationships.
- Provide PDF wireframes for inbox, compose dropdown, message detail, and
  persistent read/unread state. Team D's PDFs intentionally cover D screens only.
- Review case-duplicate account handling together with Team D, preserving the
  recipient-isolation guard; do not remove that guard to hide the error.

### Shared test/document review

- Close the four uncovered methods to meet the assignment's 100% target.
- Correct suite filters if unitTest must be completely isolated from SQLite.
- Confirm whether the rubric's >95% is measured on all tests or unit-only tests;
  do not claim the lower isolated-unit percentage meets >95%.
- Keep whole-system UML current, use PDFs where requested, and maintain schema
  columns in notes rather than inserting them into the conceptual ERD.
- Confirm every teammate has run the final built JAR.

## Specific shared-document inaccuracies to hand off

These files were inspected but left unchanged:

| Shared file | Exact correction needed |
| --- | --- |
| `README.md`, Agent messaging section | Add current web inbox/compose/message-detail routes and read/unread behavior; the section currently describes terminal messaging only. The linked Team D notes now provide current schema/run evidence, but do not replace the missing web instructions. |
| `docs/README.md`, final paragraph | It says messaging wireframes are needed when messaging becomes available; messaging is already integrated. Link current PDFs and state the remaining messaging-screen gap accurately. |
| `docs/HW7-TeamB-WorkPlan.txt`, lines 23 and 89 | Countries are described as a future dependency even though country forms/routes are implemented. |
| `docs/HW7-TeamB-WorkPlan.txt`, line 80 | The blanket claim that every B class has 100% method coverage is no longer current: the two-argument PageHandler constructor is missed in the latest full report. |
| `docs/data-models/DataModelOverview.txt`, lines 4-5 and entity list | The shared PDF no longer inventories the exact current system; web messaging classes and Message read state are missing there. The overview itself does not describe the integrated message/read-state model. |
| `docs/filehandler.txt`, constructor/method list | The directory-taking overload is incorrectly listed as a second FileHandler(); actual signature is FileHandler(String dataDirectory). writeFile(String, String): boolean is missing from the API notes. |
| `docs/HW7-ClassDiagram.pdf` | Add the actual current messaging/startup classes, read flag, methods, and associations; see Team C handoff above. Team D's scoped PDF does not silently replace this whole-system deliverable. |

The Cipher, CipherKey, CredentialManager, CredentialManagerLibrary, and
DebugHelper notes were also read. No additional substantive API mismatch was
established for those notes in this review; that is not a claim of perfect
legacy architecture or exhaustive behavioral verification.

## Suggested group message

> I rechecked NewDatabaseManagerRefactor: all 483 tests and the build pass. The
> current full-suite method coverage is 99.15%, so we still miss HW7's 100% target.
> I updated Team D's docs, added D-only diagram/wireframe PDFs, and filled teams.txt
> with our computing IDs. Shared UML still needs the messaging additions, and we
> need Mike's messaging screen wireframes plus the root README refresh. Zaid's
> help option is still a stub (string comparison/path/empty help file), and the
> navbar has Facilities twice. I also documented model/import/account edge cases
> without changing anyone's production code. Please check the handoff in
> docs/team-d/DELIVERABLES.md and each run the final JAR.

This is a draft for Vrisag to send; no message was sent by the agent.
