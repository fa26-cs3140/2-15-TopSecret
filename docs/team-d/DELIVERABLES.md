# Final working-branch deliverables review

Reviewed 2026-11-20. Source snapshot: `65e2fd2`, including shared branch
NewDatabaseManagerRefactor through `5b8c827`. PR #30 fixes are merged. The coverage follow-up must also be merged before
this report applies to submission.

## Rubric evidence

| Requirement | Evidence / final result |
| --- | --- |
| Current README, run instructions, schema | Root README, docs index, DESIGN physical schema updated |
| All names, computing IDs, GitHub usernames | Root teams.txt contains all four supplied identities |
| Current system UML PDF | Regenerated from compiled declarations including RecordImport and updated persistence API; existing messaging/read-state declarations retained |
| ERD with entities/relationships, no columns | TeamD-ERD.pdf and DESIGN entity-only diagram; column DDL kept separately |
| Current wireframes | Shared HW7-Wireframe.pdf has records/countries/inbox/compose; Team D PDF covers related data/search/session administration; see message-detail handoff |
| 100% method coverage | Complete suite 475/475; unit task is also 475/475 (100%), measured separately |
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

517 tests passed; zero failures, errors, or skips. The separate tasks passed
413 unit and 104 integration tests. Build passed. Complete-suite coverage is
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

### Final follow-up on shared snapshot 6af24d1

> The coverage gap is closed: 517 tests pass, build passes, and both the unit-task
> and complete-suite JaCoCo reports show 475/475 methods (100%). New tests use
> mocked HTTP exchanges/services/JDBC; no coverage filters were changed. Zaid's
> lowercase username policy, duplicate-navigation fix, and latest wireframe are
> included. PR #30 has already been merged. Merge the final-unit-coverage follow-up
> before submission. The previous low-unit-coverage handoff no longer applies.

Terminal editing remains a previous-assignment carry-over question, distinct
from HW7 web editing. Each member must still confirm their own system test.

### All members

> Please each run the final packaged JAR and confirm your own system test, as the
> assignment requires. The feature branch must be merged into
> NewDatabaseManagerRefactor before these results describe the submission.

No teammate message was sent automatically. This report distinguishes passing
observed behavior from remaining docs/carry-over questions; it does not promise
an error-free system or a guaranteed grade.
