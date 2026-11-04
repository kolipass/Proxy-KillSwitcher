# proxykillswitcher

Android/Kotlin app. Metro DI, Orbit MVI, Jetpack Compose.

- `:app` — root Metro graph and activity
- `:core:*` — shared infrastructure
- `:feature:*` — feature modules (`data` / `domain` / `presentation` / `di` / `navigation`)

Numbered rules live in `.skills/`, not here. This file is the index. Nested `AGENTS.md` files in modules only point at the matching orchestrator.

## How to apply skills

1. Read the orchestrator `SKILL.md` for the layer you are editing.
2. Read every sub-skill whose `when` matches the current edit.
3. Follow those numbered rules. Do not invent extra style, and do not copy rules into `AGENTS.md`.
4. On every Kotlin edit also read `.skills/kotlinStyle/SKILL.md` (selfDocumenting applies unconditionally).

Catalog of rule-id ranges: `.skills/process-tinker/skill_catalog.yaml`.

New rules and sub-skills go through `/process-tinker`. Never add a rule by editing this file.

## Process skills

Read the skill and follow its Required Algorithm. Do not skip steps.

| When | Skill |
|---|---|
| `/process-scout`, research a problem, discuss a future PRD | `.skills/process-scout/SKILL.md` |
| `/process-planner`, «Составь детальный план», compose an Implementation Plan | `.skills/process-planner/SKILL.md` |
| `/process-stylist`, bring the branch in line with project style | `.skills/process-stylist/SKILL.md` |
| `/process-tinker`, add a rule, add a sub-skill, author a skill | `.skills/process-tinker/SKILL.md` |

Scout writes a PRD. Planner writes a plan. Neither writes product code.

## Code domains

| When | Orchestrator |
|---|---|
| Pref, Storage, Loader, Repository | `.skills/datasource/SKILL.md` |
| UseCase, AvailabilityConfig, AvailabilityUseCase, PreLoader | `.skills/domain/SKILL.md` |
| Orbit ViewModel, Compose UiScreen | `.skills/presentation/SKILL.md` |
| `@Inject`, `@SingleIn`, `@Provides`, BindingContainer | `.skills/diMetro/SKILL.md` |
| Navigation wiring | `.skills/navigation/SKILL.md` |
| `*Test.kt`, `src/test/` | `.skills/tester/SKILL.md` |

## Docs

| Artifact | Standard | Location |
|---|---|---|
| PRD | `.skills/docs/prd/SKILL.md` | `.claude/prd/<slug>/<name>.yaml` |
| Implementation Plan | `.skills/docs/plan/SKILL.md` | `.claude/prd/plan_<two_word_name>.yaml` |
| Feature readme | `.skills/docs/readme/SKILL.md` | `readme.yaml` next to the feature |

Validators:

```bash
python3 .skills/docs/tools/validate_prd.py <prd.yaml>
python3 .skills/docs/tools/validate_plan.py <plan.yaml>
```

## GitNexus

For architecture, blast radius, debugging, or refactoring, start at `.skills/gitnexus/gitnexus-guide/SKILL.md` and follow the matching gitnexus skill. Do not fall back to grep when GitNexus is available.

## Git & Delivery Process (MR/PR Only)

All changes MUST go through Pull/Merge Requests. Direct commits and pushes to `main` are strictly forbidden:

1. **Never commit directly to `main`**: Before making changes, ensure you are in a dedicated feature/fix branch created from `main`.
2. **Branch naming**: Use `feature/<name>` for new functionality, `fix/<name>` for bug fixes, or `docs/<name>` / `refactor/<name>` where appropriate (e.g. `feature/http-killswitch`, `fix/vpn-reconnect`).
3. **Delivery workflow**:
   - Create and checkout branch: `git checkout -b feature/<name>` (branched off up-to-date `main`).
   - Implement changes and run verification: `./gradlew ktlintCheck detekt test`.
   - Commit with clear message.
   - Push feature branch to origin: `git push -u origin feature/<name>`.
   - Prepare MR/PR against `main` for review.

## Verify

```bash
./gradlew ktlintCheck detekt test
```

ktlint and detekt own formatting, imports, and indentation. Skill rules own the rest.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **proxykillswitcher** (14042 symbols, 23347 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/proxykillswitcher/context` | Codebase overview, check index freshness |
| `gitnexus://repo/proxykillswitcher/clusters` | All functional areas |
| `gitnexus://repo/proxykillswitcher/processes` | All execution flows |
| `gitnexus://repo/proxykillswitcher/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
