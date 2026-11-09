# AI-assisted development rules

These rules apply to every AI tool used on this project (Antigravity, Codex, Claude).

1. **Docs first.** Update `docs/` before implementing a design change, and keep implemented and
   planned functionality clearly separated. Do not document integrations that do not exist.
2. **Stay in scope.** Work only on the current phase in `development-roadmap.md`. Do not add later-phase
   features (events, saga, payments, contracts) early.
3. **One module or task per session.** Give a tool a bounded task, not the whole system.
4. **Tests ship with features.** Every implemented feature gets automated tests covering normal use,
   validation failures and not-found or conflict cases.
5. **Verify, don't assume.** Run the build and tests yourself. Never claim a build, test or Docker run
   succeeded unless it was actually executed, and report what was not run.
6. **Review every diff** before committing. You must be able to explain every line, since it will be
   questioned by faculty and interviewers.
7. **Migrations are append-only.** Never edit an applied Flyway migration; add a new one.
8. **No secrets in the repository.** No private keys, wallet keys, API keys or real credentials.
   Local Docker credentials are development-only and must be labelled as such. Smart contracts use
   test networks only.
9. **Check versions against official sources** before adding or upgrading a dependency.
10. **Small, clear commits** with one purpose each, after each verified step.
