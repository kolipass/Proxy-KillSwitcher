---
name: docs/plan
description: Canonical YAML standard for Implementation Plans.
---

## Purpose

An Implementation Plan is the task breakdown the planner agent writes from an accepted PRD. Each task
carries two layers: a removable human layer in Russian that lets a reviewer approve the plan
before code exists, and an English agent layer that must stay self-sufficient once the human
layer is stripped — the executor agent's entire context is the PRD plus this plan. The exact
field names, formatting, and verification rules are defined in plan_standard_prd.yaml.

## Reference example

Read `.skills/docs/plan/plan_standard_prd.yaml` — the PRD behind this standard.

```yaml
rules:

    -   id: IP01
        rule: Plan lives at .claude/prd/plan_<two_word_name>.yaml
        details: discovery matches the full plan_<w>_<w>.yaml shape, not a prefix/suffix
            heuristic — a plan's own second word may itself be "prd"
        do: |
            .claude/prd/plan_launcher_detect.yaml
            # the plan sits beside its PRD in .claude/prd/; the plan_ prefix separates it
            # from PRD files (*_prd.yaml) so discovery never confuses the two
        dont: |
            .claude/prd/launcher_detect_prd.yaml
            # that name belongs to a PRD — a plan always carries the plan_ prefix
            12-0506_launcher_detect.yaml
            # no date-time prefix — one plan per PRD; replanning overwrites the file

    -   id: IP02
        rule: Top-level key order is prd -> title -> tasks -> verify
        do: |
            prd: ...
            title: "..."
            tasks:
                - ...
            verify: |
                ...
        file_reference: .skills/docs/plan/plan_standard_prd.yaml

    -   id: IP03
        rule: prd is the path to the source PRD; the file must exist
        do: |
            prd: feature/<name>/<two_words>_prd.yaml
            # consumer projects keep PRDs in .claude/prd/ instead;
            # the executor's whole context is this PRD plus the plan

    -   id: IP04
        rule: Plan title is a one-line double-quoted English string
        do: |
            title: "Introduce two-layer plan standard with validator and tests"
            # pipelines read title via a YAML parser; the quotes mark it as
            # human-read (it becomes the commit message)
        dont: |
            title: >
                Introduce two-layer plan standard with validator and tests

    -   id: IP05
        rule: tasks is a non-empty list with ids S-1..S-n sequential from 1
        do: |
            tasks:
                -   id: S-1
                -   id: S-2

    -   id: IP06
        rule: Task key order is id, title, review, introduces, skills, files_to_create,
            files_to_modify, constraints, depends_on, acceptance; empty optionals omitted
        do: |
            # introduces, files_to_create, files_to_modify, constraints are optional —
            # omit the key entirely when it has nothing to say

    -   id: IP07
        rule: Task title is a one-line double-quoted English string
        do: |
            title: "Create validate_plan.py validator"
        dont: |
            title: >
                Create validate_plan.py validator

    -   id: IP08
        rule: review is required, Russian, double-quoted opened on the key's line
        do: |
            review: "Валидатор нового стандарта по образцу validate_prd.py: те же CLI-режимы,
                тот же стиль ошибок."
            # 2-6 sentences a reviewer understands without reading code;
            # wrapping onto next lines is fine, ">" and "|" blocks are not

    -   id: IP09
        rule: introduces is an optional list of {name, kind, at}; kind is a fixed enum
        do: |
            introduces:
                -   name: "validate_plan.py"
                    kind: "file"
                    at: ".skills/docs/tools/"
            # kind: file | class | function | var | flag | rule | endpoint

    -   id: IP10
        rule: Human layer (review, introduces) must be strippable without agent context loss
        do: |
            # any fact the executor needs lives in the agent layer, even if that
            # repeats the human layer — automation deletes review/introduces
        dont: |
            review: "История v1..v26 неизменяема — агент, не трогай её!"
            # a prohibition the executor must obey belongs in constraints (IP14)

    -   id: IP11
        rule: Every introduces name appears verbatim in the same task's agent fields
        do: |
            # renaming during review = edit introduces AND the matching
            # files_to_create/files_to_modify/constraints text before implementation

    -   id: IP12
        rule: skills is a required list of {path, rules} pinned last, once the task's code is designed
        do: |
            skills:
                -   path: .skills/datasource/repository/SKILL.md
                    rules: [R01]
                -   path: .skills/kotlinStyle/selfDocumenting/SKILL.md
                    rules: [SD01, SD02]
            # fill skills as the final authoring pass, when files_to_create/files_to_modify
            # are already written: pin every skill the task's code touches (DI, repository,
            # tests, style, ...) with its load-bearing rule ids — not just the generic style
            # skill; path must exist; rules may be [] only for skills without numbered rules;
            # skills: [] is allowed when no skill applies

    -   id: IP13
        rule: files_to_create entries are {path, description}; files_to_modify are {path, changes}
        do: |
            files_to_modify:
                -   path: docker-compose.yml
                    changes: >
                        Delete the runner-claude service block; leave postgres untouched.
            # mechanics live only here — a task has no free-form description field

    -   id: IP14
        rule: constraints is the only home for prohibitions and DO-NOT-CHANGE invariants
        do: |
            constraints:
                - >
                    history v1..v26 in <two_words>_prd.yaml is immutable — append one new
                    entry, never edit existing ones
        dont: |
            acceptance:
                - history entries were not modified
            # prohibitions never hide in review or acceptance

    -   id: IP15
        rule: depends_on is required, [] if none, references only earlier task ids
        do: |
            depends_on: []
            # or: depends_on with S-ids strictly lower than the task's own id

    -   id: IP16
        rule: acceptance is required — 1-3 testable done-conditions
        do: |
            acceptance:
                - validate_plan.py exits 0 on a well-formed fixture
            # agent text — plain, no double quotes (IP18)

    -   id: IP17
        rule: Last task is a mandatory process-stylist pass over the whole branch diff
        do: |
            tasks:
                -   id: S-n   # process-stylist pass — the last task
                    files_to_modify:
                        -   path: whole branch diff
                            changes: >
                                Follow .skills/process-stylist/SKILL.md. Audit set is the diff
                                against git merge-base main HEAD plus uncommitted changes; fix only
                                violations citing a rule id, then run the validators once.
                    constraints:
                        - >
                            Never change behavior; no refactoring beyond cited violations.
            # the pointer plus one summary line is the whole payload — the procedure itself
            # lives in the skill and is never copied into the plan; the audited files are
            # unknown at planning time, so the symbolic path is never expanded

    -   id: IP18
        rule: Double quotes mark the human-read fields; agent text is never double-quoted
        do: |
            review: "Человеческий слой — в кавычках, в IDE подсвечен зелёным."
            title: "Create validate_plan.py validator"
            changes: >
                Agent prose stays plain white — one line or a folded ">" block,
                whichever reads better.
            acceptance:
                - validate_plan.py exits 0 on a well-formed fixture
        dont: |
            changes: "Replace the entry"
            acceptance:
                - "quoted agent text renders green and reads as human layer"

    -   id: IP19
        rule: Human fields are Russian; agent fields are English; identifiers stay verbatim
        do: |
            review: "Планер переводится на новый стандарт: читает docs/plan перед записью."
            changes: >
                Point step 6 at .skills/docs/plan/SKILL.md.
            # paths, commands and code identifiers are never translated

    -   id: IP20
        rule: Lines <=120 chars; keys plain snake_case
        do: |
            depends_on: []
        dont: |
            ⚠️ read_before_implementation: ...
            # no emoji or decorations in keys; wrap prose within 120 chars

    -   id: IP21
        rule: Exactly one top-level verify block, tied to the last task; per-task verify forbidden
        do: |
            tasks:
                -   id: S-n
                    acceptance:
                        - every command in the top-level verify block exits 0
            verify: |
                ./gradlew ktlintCheck detekt test
            # verify is one top-level block at the end holding its commands ("|" allowed
            # only here) — for Kotlin plans: ktlint, detekt, unit tests; for other stacks,
            # that stack's own validators. The repair cap lives in the skill's run_validators
            # step, never in the plan
        dont: |
            tasks:
                -   id: S-1
                    verify: ./gradlew lint
            # per-task verify is forbidden — the plan validates once, after every task
```

- Validator: `python3 .skills/docs/tools/validate_plan.py <plan.yaml>` (PyYAML required).
