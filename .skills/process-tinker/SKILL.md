---
name: process-tinker
description: |
    Keeper of skill/rule authoring: orchestrator format, rule format contract, and how a
    described recurring pattern becomes a proposed rule.
    Trigger: `/process-tinker` — always activates this skill.
    Auto-invocation: any request to add a rule, add a sub-skill, or author a new skill.
---

## Skill naming

A sub-skill's frontmatter `name` must equal its `<orchestrator>/<sub-skill>` directory path under `.skills/`.

All skills and sub-skill categories are named in CamelCase (e.g. `availabilityConfig`), except process
skills — the top-level `process-<name>` orchestrators carrying a Required Algorithm — which keep their
kebab-case `process-` prefix.

## Orchestrator format

Orchestrator skill files carry no rules of their own — each rule lives in a sub-skill below them.
Before any `<domain>` task, read every sub-skill whose `when` matches and follow its rules.

```yaml
sub_skills:
  - name: <orchestrator>/<sub-skill>
    path: .skills/<orchestrator>/<sub-skill>/SKILL.md
    when: compact English routing trigger, ≤12 words
```

## How to Add a New Sub Skill

```yaml
how_to_add:
  - id: 1
    action: Choose an id prefix (2-3 uppercase letters)
    note: see skill catalog below
  - id: 2
    action: Create .skills/<skill>/<category>/SKILL.md
    note: frontmatter + a single yaml rules block
  - id: 3
    action: Add entry to the sub-skills tree
    note: in .skills/<skill>/SKILL.md
```

## How to Add a Rule to an Existing Skill

```yaml
how_to_add_rule:
  - id: 1
    action: Follow rule_format section below when writing the rule
  - id: 2
    action: Append rule following rule_format constraints
  - id: 3
    action: Update id_range in .skills/process-tinker/skill_catalog.yaml
  - id: 4
    action: python3 -m pytest .skills/process-tinker/tools/test_skill_catalog.py
    note: verifies the catalog range against the ids actually present in the file
```

## Rule Format

```yaml
file_layout:
  note: >
    a sub-skill SKILL.md is frontmatter + a single yaml block opening directly with
    `rules:` — no meta block; id ranges live only in skill_catalog.yaml
  severity: every rule is mandatory — there is no severity field

id:
  format: PREFIX + sequential number (e.g. N01, CR12)
  rule: prefix per category, sequential within category
rule: 4–9 words, concise and specific
do:
  format: minimal correct code example — 3 lines max
  rule: required on every rule
dont:
  format: minimal anti-pattern code example — 3 lines max
  rule: only when without it a concrete error is likely
details:
  format: 5–30 words
  rule: only if no file_reference
file_reference: file path
constraints:
  - details and file_reference are mutually exclusive
  - details / file_reference — no more than 10% of total rules
  - dont — no more than 25% of total rules; omit if rule text alone is sufficient
  - do / dont — generic Example* names, never a real project class, field or key name
```

## Required Algorithm

```yaml
algorithm:

    -   step: 0
        name: intake
        actions:
            -   id: 1
                action: Accept a plain-text description of a recurring pattern as input
                note: >
                    the description's source is not fixed by this skill — GitLab MR
                    comments, a manual write-up, anything else; reading a source system
                    directly is out of scope here

    -   step: 1
        name: locate_target
        actions:
            -   id: 1
                action: Read .skills/process-tinker/skill_catalog.yaml
                note: matches the pattern to a domain/sub-skill by its id prefix and scopes
            -   id: 2
                action: >
                    If no existing sub-skill fits, determine that a new sub-skill or a new
                    category is needed, following "How to Add a New Sub Skill" above

    -   step: 2
        name: draft_rule
        actions:
            -   id: 1
                action: Compose a candidate rule strictly per the Rule Format section above
            -   id: 2
                action: Compute the next sequential id from the category's id_range in skill_catalog.yaml

    -   step: 3
        name: confirm
        actions:
            -   id: 1
                action: >
                    Print the full candidate rule text, the target file it would be appended
                    to, and the id_range bump in skill_catalog.yaml
            -   id: 2
                action: Stop and wait for explicit user confirmation
        constraints:
            - never proceed to step 4 without explicit confirmation

    -   step: 4
        name: write
        actions:
            -   id: 1
                action: Append the rule to the target file, following "How to Add a Rule" above
            -   id: 2
                action: Update the matching entry in skill_catalog.yaml
        constraints:
            - process-tinker never calls Edit or Write before step 3's confirmation is explicit
```
