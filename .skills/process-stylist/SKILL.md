---
name: process-stylist
description: |
    Audits the whole branch diff against the project's own skill rules and fixes what violates them.
    Trigger: `/process-stylist` — always activates this skill.
    Auto-invocation: any request to bring the current branch in line with the project's style.
tools: Read, Edit, Write, Grep, Glob, Bash, Task.
---

## Role

Stylist owns the quality pass over a finished branch: it never designs, never adds behaviour and
never refactors on its own initiative. Every edit it makes traces back to one numbered rule in one
domain `SKILL.md` of the project it runs in. The rules are the project's, not the stylist's — this
skill carries no rule ids of its own and is never registered in the category registry.

Sub-agents are used for the audit only, and only read-only: findings come back to the parent, and
the parent is the only writer.

## Required Algorithm

```yaml
algorithm:

    -   step: 0
        name: collect_diff
        actions:
            -   id: 1
                action: >
                    Build the audit set: git diff --name-only against git merge-base main HEAD,
                    plus uncommitted changes
                note: the merge base, not main itself — commits landing on main meanwhile are not this branch
            -   id: 2
                action: Stop and report "nothing to audit" when the audit set is empty
                note: an empty branch diff is a valid outcome, never a reason to widen the scope

    -   step: 1
        name: select_domains
        note: >
            the registry lists domains that may not exist in this project, may be placeholders,
            or may not be touched by this diff — the list is a candidate set, never the answer
        actions:
            -   id: 1
                action: Read scopes from .skills/process-tinker/skill_catalog.yaml
            -   id: 2
                action: Keep a domain only when all four conditions hold
                conditions:
                    - the domain is listed under scopes
                    - .skills/<domain>/SKILL.md exists in this project
                    - that file carries numbered rules, not a placeholder
                    - the audit set contains files the domain governs
            -   id: 3
                action: Stop and report when no domain survives the filter
                note: never audit a domain with an empty rule set — an auditor without rules invents them

    -   step: 2
        name: parallel_audit
        actions:
            -   id: 1
                action: Spawn one read-only audit sub-agent per surviving domain
                note: >
                    in parallel when the runtime supports sub-agents, sequentially otherwise;
                    each agent gets only its own domain SKILL.md and only its slice of the diff
            -   id: 2
                action: Give every agent the same reporting contract
                finding_contract:
                    - file
                    - line
                    - rule_id
                    - the verbatim rule text quoted from that SKILL.md
                    - the diff fragment that violates it
            -   id: 3
                action: Tell every agent not to report what a formatter or a linter already catches
                note: formatting, import order and indentation belong to ktlint and detekt, not here
        constraints:
            - audit sub-agents never edit files and never ask the user
            - a domain is never audited against another domain's rules

    -   step: 3
        name: verify_findings
        note: >
            audit agents are the same model as everyone else — they misspell rule ids, cite real
            rules for code they do not govern, and name files that are not in the diff
        actions:
            -   id: 1
                action: Check every finding mechanically with grep, never with a model call
                checks:
                    - rule_id exists in that domain's SKILL.md
                    - the quoted rule text matches that file verbatim
                    - the diff fragment is present in the audit set diff
                    - the file is inside the audit set
            -   id: 2
                action: Drop every finding that fails any check, silently and without repair
                note: a finding that cannot prove itself is not evidence of anything

    -   step: 4
        name: fix_violations
        actions:
            -   id: 1
                action: Fix only the findings that survived step 3
            -   id: 2
                action: Report the dropped and the fixed counts per domain
        constraints:
            - never change behaviour or any public interface
            - never refactor beyond the cited violation
            - never touch a file outside the audit set
            - never run stack validators here — step 5 is the only place they run

    -   step: 5
        name: run_validators
        note: >
            one run after every domain is fixed, never after a single edit or a single domain:
            per-fix validation multiplies the run time by the number of findings for the same result
        actions:
            -   id: 1
                action: Run the target stack's validators once
                note: Kotlin projects — ktlint, detekt and the unit tests; another stack — its own validators
            -   id: 2
                action: On failure, fix and rerun the validators in full
                note: >
                    detekt — rerun with -PdetektAutoCorrect=true; ktlint — rerun via ktlintFormat;
                    fix by hand only what survives that; rerun the full suite, not just what failed
            -   id: 3
                action: Stop and report the failure after two failed repair attempts
```
