# proxykillswitcher

Android/Kotlin app. Metro DI, Orbit MVI, Jetpack Compose.

- `:app` — root Metro graph and activity
- `:core:*` — shared infrastructure
- `:feature:*` — feature modules (`data` / `domain` / `presentation` / `di` / `navigation`)

Numbered rules live in `.cursor/skills/`, not here. This file is the index. Nested `AGENTS.md` files in modules only point at the matching orchestrator.

## How to apply skills

1. Read the orchestrator `SKILL.md` for the layer you are editing.
2. Read every sub-skill whose `when` matches the current edit.
3. Follow those numbered rules. Do not invent extra style, and do not copy rules into `AGENTS.md`.
4. On every Kotlin edit also read `.cursor/skills/kotlinStyle/SKILL.md` (selfDocumenting applies unconditionally).

Catalog of rule-id ranges: `.cursor/skills/process-tinker/skill_catalog.yaml`.

New rules and sub-skills go through `/process-tinker`. Never add a rule by editing this file.

## Process skills

Read the skill and follow its Required Algorithm. Do not skip steps.

| When | Skill |
|---|---|
| `/process-scout`, research a problem, discuss a future PRD | `.cursor/skills/process-scout/SKILL.md` |
| `/process-planner`, «Составь детальный план», compose an Implementation Plan | `.cursor/skills/process-planner/SKILL.md` |
| `/process-stylist`, bring the branch in line with project style | `.cursor/skills/process-stylist/SKILL.md` |
| `/process-tinker`, add a rule, add a sub-skill, author a skill | `.cursor/skills/process-tinker/SKILL.md` |

Scout writes a PRD. Planner writes a plan. Neither writes product code.

## Code domains

| When | Orchestrator |
|---|---|
| Pref, Storage, Loader, Repository | `.cursor/skills/datasource/SKILL.md` |
| UseCase, AvailabilityConfig, AvailabilityUseCase, PreLoader | `.cursor/skills/domain/SKILL.md` |
| Orbit ViewModel, Compose UiScreen | `.cursor/skills/presentation/SKILL.md` |
| `@Inject`, `@SingleIn`, `@Provides`, BindingContainer | `.cursor/skills/diMetro/SKILL.md` |
| Navigation wiring | `.cursor/skills/navigation/SKILL.md` |
| `*Test.kt`, `src/test/` | `.cursor/skills/tester/SKILL.md` |

## Docs

| Artifact | Standard | Location |
|---|---|---|
| PRD | `.cursor/skills/docs/prd/SKILL.md` | `.claude/prd/<slug>/<name>.yaml` |
| Implementation Plan | `.cursor/skills/docs/plan/SKILL.md` | `.claude/prd/plan_<two_word_name>.yaml` |
| Feature readme | `.cursor/skills/docs/readme/SKILL.md` | `readme.yaml` next to the feature |

Validators:

```bash
python3 .cursor/skills/docs/tools/validate_prd.py <prd.yaml>
python3 .cursor/skills/docs/tools/validate_plan.py <plan.yaml>
```

## GitNexus

For architecture, blast radius, debugging, or refactoring, start at `.cursor/skills/gitnexus/gitnexus-guide/SKILL.md` and follow the matching gitnexus skill. Do not fall back to grep when GitNexus is available.

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
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
