# Git & Delivery Process (MR/PR Only)

All changes MUST go through Pull/Merge Requests. Direct commits and pushes to `main` are strictly forbidden:

1. **Never commit directly to `main`**: Before making changes, ensure you are in a dedicated feature/fix branch created from `main`.
2. **Branch naming**: Use `feature/<name>` for new functionality, `fix/<name>` for bug fixes, or `docs/<name>` / `refactor/<name>` where appropriate (e.g. `feature/http-killswitch`, `fix/vpn-reconnect`).
3. **Delivery workflow**:
   - Create and checkout branch: `git checkout -b feature/<name>` (branched off up-to-date `main`).
   - Implement changes and run verification: `./gradlew ktlintCheck detekt test`.
   - Commit with clear message.
   - Push feature branch to origin: `git push -u origin feature/<name>`.
   - **Create Pull Request immediately as part of delivery (`.skills/process-delivery/SKILL.md`)**:
     - **Title**: short, concise summary (e.g. `feat(killswitch): add socks5 authentication`).
     - **Description**: mandatory structured summary of incoming changes:
       - `### Что изменилось` (bullet points explaining what came in the PR)
       - `### Зачем` (motivation/goal)
       - `### Проверка` (validation commands and results)
     - Present the PR link and incoming changes description to the user.

## Agent Worktree Isolation

All new work delegated to subagents MUST be started in a dedicated git worktree — never in the user's main working tree:

1. **Always spawn subagents with `isolation: "worktree"`** so each agent works on its own isolated copy of the repo. This prevents conflicts with uncommitted changes in the user's tree and allows multiple agents to work in parallel without stepping on each other.
2. **Never let a subagent modify files in the main working tree** unless the user explicitly asks for it.
3. A worktree that ends up unchanged is auto-cleaned; if the agent produced changes, surface its branch/worktree path to the user and integrate the changes via the standard MR/PR workflow above (never merge into `main` directly).
4. Work done directly by the primary session (not delegated to a subagent) still follows the branch-naming and MR/PR rules above.

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
- NEVER commit or push directly to `main` branch — all changes must go through dedicated feature branches (`feature/<name>`, `fix/<name>`) and Pull Requests / Merge Requests.

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
