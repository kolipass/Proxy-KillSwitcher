---
name: docs
description: Orchestrator — standardizes PRDs, Implementation Plans and readme.yaml.
---

```yaml
rule: Read every sub-skill whose `when` matches and follow its rules.
sub_skills:
    -   name: docs/prd
        path: .cursor/skills/docs/prd/SKILL.md
        when: writing or normalizing a PRD (product requirement document)

    -   name: docs/readme
        path: .cursor/skills/docs/readme/SKILL.md
        when: writing or normalizing a readme.yaml

    -   name: docs/plan
        path: .cursor/skills/docs/plan/SKILL.md
        when: writing or validating an Implementation Plan
```
