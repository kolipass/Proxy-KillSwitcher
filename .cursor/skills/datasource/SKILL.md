---
name: datasource
description: Orchestrator for the data layer — creates and edits Preferences, Storage, Loader and Repository classes.
---

```yaml
rule: Read every sub-skill whose `when` matches and follow its rules.
sub_skills:
    -   name: datasource/storage
        path: .cursor/skills/datasource/storage/SKILL.md
        when: creating or edit Pref, Storage classes

    -   name: datasource/repository
        path: .cursor/skills/datasource/repository/SKILL.md
        when: creating or edit Repository classes

    -   name: datasource/loader
        path: .cursor/skills/datasource/loader/SKILL.md
        when: create or edit Remote Loader / Local Loader classes
```
