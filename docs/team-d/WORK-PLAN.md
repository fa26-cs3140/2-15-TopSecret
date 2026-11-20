# Team D work plan and outstanding backlog

Team D provides complete-record persistence/audit, related-record resolution,
all-field agency-record search, and session access control. The Week 1 design
and test-contract commits and Week 2 implementation commits remain in Git.
The current integrated branch is NewDatabaseManagerRefactor.

## Implemented and exercised

| Feature | Evidence |
| --- | --- |
| Atomic content changes | RecordEditor + SqliteRecordStore; forced audit rollback tests |
| Audit attribution | Username, action, type, ID, UTC timestamp persisted |
| Related navigation | Country/facility, facility/agent/mission, mission/brief/agent links |
| Terminal search | RecordSearch searches all current agency-record text fields |
| Session lifecycle | Random tokens, sliding 30-minute expiry, permission reload |
| Session administration | Admin terminal option 12 lists user/token/activity and revokes |
| Country compatibility | Seed/restart/migration, reference validation, renaming/reassignment |
| Shared edit compatibility | AppController uses one transactional boundary for web and terminal creates |
| Integration tests | Real HTTP/SQLite workflows plus class-pair integration test examples |

## Remaining Team D and shared model backlog

| Priority | Finding | Affected boundary |
| --- | --- | --- |
| High | Partial TSV imports produce misleading audit counts | Managers + terminal import workflow |
| High | Quoted brief/agent lookups can silently lose mission links | BriefManager/AgentManager + terminal mission prompt |
| Medium | Read failures become empty lists | BriefManager/AgentManager and callers |
| Medium | Failed first password-file write strands bootstrap | UserAccountManager + FileHandler |
| Medium | Case-duplicate account creation blocks messaging | Account uniqueness + Team C identity guard |
| Medium | Incomplete form request blocks other clients | Shared WebServer execution/body-reading boundary |
| Grading | Four methods untested in complete suite | Shared web/static/startup code |

No production fixes are included in this documentation update. Passing normal
workflows and 61/61 measured core-service methods do not close these failures.

## Team handoff

Team C's web messaging and read indicators are now integrated; they are no longer
future work. Team A's CSS is integrated, including its corrected audit text color.
Team A's help option is only a stub and remains incomplete.

The current global UML, root README, and full messaging wireframes still need
their owners' update. See DELIVERABLES.md for the exact rubric matrix and a
ready-to-send handoff. The shared roster is at ../../teams.txt with the computing
IDs supplied by the team. Team D's scoped PDFs are current; they are not a claim
that the shared whole-system class diagram is current.

Each member should run the final packaged JAR personally and review the final
combined diagrams before submission. Changes should remain small, attributable
commits on feature branches, with PRs targeting NewDatabaseManagerRefactor.
