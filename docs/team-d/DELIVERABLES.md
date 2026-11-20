# Final working-branch deliverables review

Reviewed 2026-11-20. Source snapshot: `65e2fd2`, including shared branch
NewDatabaseManagerRefactor through `5b8c827`. This feature branch includes the
Team D/shared fixes; they must be merged before this report applies to submission.

## Rubric evidence

| Requirement | Evidence / final result |
| --- | --- |
| Current README, run instructions, schema | Root README, docs index, DESIGN physical schema updated |
| All names, computing IDs, GitHub usernames | Root teams.txt contains all four supplied identities |
| Current system UML PDF | Regenerated from compiled declarations including RecordImport and updated persistence API; existing messaging/read-state declarations retained |
| ERD with entities/relationships, no columns | TeamD-ERD.pdf and DESIGN entity-only diagram; column DDL kept separately |
| Current wireframes | Shared HW7-Wireframe.pdf has records/countries/inbox/compose; Team D PDF covers related data/search/session administration; see message-detail handoff |
| 100% method coverage | Complete suite 475/475; isolated unit task is 422/475 (88.84%), disclosed separately |
| Unit tests/Mockito | Service, account, search, relationship, and JDBC boundary mocks; some legacy tests remain misclassified by filename filters |
| Integration test ClassAClassB naming | AgencyInterfaceAuditLogManagerTest and AgencyInterfaceMissionManagerTest integrate real components |
| Web add/edit, admin only | Real HTTP/SQLite acceptance, permissions, invalid-input and rollback tests pass |
| Countries | 196 supplied enum names verified against data/countries.txt; create/edit/rename/reassignment/restart pass |
| Related clickable records | All implemented agency relationships tested; quoted brief/agent terminal selection repaired |
| Audit changes | Atomic CREATE/UPDATE; imports now add per-row CREATE and aggregate IMPORT in same transaction; READ retained |
| Four-member web messaging | Real HTTP recipient isolation, compose, read persistence, and no administrator override pass |
| Thirty-minute session control | Exact idle boundary, sliding refresh, current roles, terminal listing and revocation pass |
| Help | Packaged -h prints help and exits without login; previous stub finding is resolved |
| Small commits | Separate lookup, account, import, fixture, server, formatting, and documentation commits |
| Individual system tests | Team D packaged workflow passed; each other teammate must confirm personally |

## Final measured runs

501 tests passed; zero failures, errors, or skips. The separate tasks passed
397 unit and 104 integration tests. Build passed. Complete-suite coverage is
475/475 methods; no production methods were excluded. The final packaged
country/session workflow, packaged help, and 307 additional adversarial checks
passed, including 80 concurrent writes and 120 parallel login/read pairs.
See VERIFICATION.md for commands, scope, and limitations.

## Repairs included in this branch

- Bind BriefManager/AgentManager lookup values and surface storage errors.
- Prevent failed first password-file writes from stranding bootstrap; attempt
  credential cleanup after metadata failure.
- Reject case-equivalent account insertions, preserving legacy recipient guards.
- Validate complete imports and commit imported content and audit as one batch.
- Isolate incomplete HTTP forms and reject bodies over 64 KiB.
- Update shared run/model/file-handler notes, roster, schema, and diagrams.

Compatibility edits to other boundaries are limited to AppController and
AgencyInterface routing imports through the audited batch store, shared
WebServer request handling, and the messaging acceptance fixture deliberately
seeding legacy ambiguous accounts. Team C's production messaging model and
its privacy behavior are unchanged. Team A's styling/country behavior is retained.
The team's latest coverage tests and PDF work were merged and preserved.

## Copy-paste owner handoffs

### Zaid — Team A

> I reran the final JAR and -h works; country create/edit, assignment, rename,
> seeds, and restart checks pass. One small UI issue remains: HtmlPage has the
> Facilities navbar link twice, and the shared wireframe repeats it. Please
> remove the duplicate and refresh the affected screen export. Your country
> functionality did not produce a failing test in this audit.

### Dillon — Team B

> Final complete suite is 501 passing and 475/475 methods covered. Your latest
> tests and UML are included. Please refresh HW7-TeamB-WorkPlan.txt: it still
> describes countries as future work and lists already-fixed swapped messaging
> options, zero-brief missions, SQL updates, and removed files as open bugs.
> Terminal record editing is still absent: it was required by the previous
> assignment, so confirm whether that carry-over is waived or needs completing.
> This is separate from the current HW7 web-editing workflow, which passes.
> If the TA requires >95% on isolated unit tests alone, we currently have 88.84%
> there, despite 100% on the complete suite; the filename filters also classify
> some real SQLite tests as units. Please clarify that grading interpretation.

### Mike — Team C

> Web messaging passed recipient privacy, no-admin-override, read-state persistence,
> compose, and parallel HTTP checks. The latest shared wireframes include inbox
> and compose; please add a message-detail screen and show the unread-to-read
> transition. New case-duplicate account creation is now blocked on our account
> side. Your model code was not changed; the acceptance fixture only seeds an
> intentionally old ambiguous identity to retain the fail-closed regression test.

### All members

> Please each run the final packaged JAR and confirm your own system test, as the
> assignment requires. The feature branch must be merged into
> NewDatabaseManagerRefactor before these results describe the submission.

No teammate message was sent automatically. This report distinguishes passing
observed behavior from remaining docs/carry-over questions; it does not promise
an error-free system or a guaranteed grade.
