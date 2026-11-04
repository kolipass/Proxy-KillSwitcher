---
name: process-planner
description: |
    Produces a detailed YAML Implementation Plan from the selected PRD file.
    Trigger phrase: "Составь детальный план" or "/process-planner" — always activates this skill.
    Auto-invocation: any request to make, create, or compose a plan.
---

## Required Algorithm

```yaml
algorithm:

  - step: 0
    name: gather_inputs
    actions:
      - id: 1
        action: Determine the PRD file to plan and read it from disk
        note: from disk, not from git
      - id: 2
        action: >
          Read the output format contract .skills/docs/plan/SKILL.md and the PRD behind it
          .skills/docs/plan/plan_standard_prd.yaml
        note: >
          the two-layer plan contract + the rationale PRD — read both here, while decomposing;
          the rationale is understood before the plan is composed, never re-read at write time
      - id: 3
        action: Scan .skills/ and read the frontmatter of every SKILL.md
        note: this is the catalog for matching subtasks to skills in step 1

  - step: 1
    name: decompose
    actions:
      - id: 1
        action: Decompose the task into subtasks S-1, S-2, ... in dependency order
      - id: 2
        action: Find the optimal solution for each subtask

  - step: 2
    name: write_plan
    actions:
      - id: 1
        action: Re-read the format contract .skills/docs/plan/SKILL.md
        note: >
          immediately before writing — never from memory; only the format contract (shape),
          not the rationale PRD, so serialization is aligned without re-litigating the plan
      - id: 2
        action: Write a valid YAML document with the Write tool
        path: .claude/prd/plan_<two_word_name>.yaml
        note: >
          <two_word_name> is a short 2-word name, e.g. launcher_detect;
          one plan per PRD — replanning overwrites the same file
      - id: 3
        action: Do not print the plan to stdout

  - step: 3
    name: pin_skills
    actions:
      - id: 1
        action: Re-read every task's agent layer (files_to_create, files_to_modify, constraints)
      - id: 2
        action: For each task pin every applicable skill from the step 0 catalog into skills
        note: >
          this pass is a critical planning stage: the pinned skills are what keeps the
          executor's code in the project's style — select them thoroughly for every task,
          once its agent layer (files_to_create, files_to_modify, constraints) is written.
          Pin every skill the planned files and changes touch (DI, repository, tests,
          style, ...) with its load-bearing rule ids; a task with only the generic style
          skill is a red flag, re-check it against the catalog

  - step: 4
    name: append_final_task
    note: >
      every plan ends with one mandatory quality task (docs/plan IP17, IP21);
      generate it from the standard's template after the real tasks are pinned
    actions:
      - id: 1
        action: >
          Append the stylist task as the last task (IP17). Its single files_to_modify entry
          uses the symbolic path "whole branch diff" — the audited files are unknown at
          planning time, so never enumerate them. Its changes text is a pointer to
          .skills/process-stylist/SKILL.md plus one summary line: the audit set is
          git diff --name-only against git merge-base main HEAD plus uncommitted changes,
          only violations citing a rule id are fixed, and the validators run once at the end.
          Its acceptance ties every command of the top-level verify block to exit 0.
        note: >
          never copy the procedure itself into the task — it lives in the skill;
          add constraints forbidding behavior/API changes and refactoring beyond cited rules
      - id: 2
        action: Pin .skills/process-stylist/SKILL.md in that task's skills
        note: rules is [] — the skill carries no numbered rules of its own
      - id: 3
        action: >
          Fill the single top-level verify with the target stack's validators — Kotlin
          projects: ktlint, detekt and unit tests; another stack: that stack's own
          validators.

  - step: 5
    name: validate
    actions:
      - id: 1
        action: Run python3 .skills/docs/tools/validate_plan.py <plan_path>
      - id: 2
        action: Fix every reported violation and re-run until exit code is 0
        note: the validator must pass before the plan is presented for review
```
