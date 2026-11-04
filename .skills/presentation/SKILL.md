---
name: presentation
description: Orchestrator — creates and edits Orbit MVI ViewModels and Jetpack Compose UI.
---

```yaml
rule: Read every sub-skill whose `when` matches and follow its rules.
sub_skills:
    -   name: presentation/viewmodel
        path: .skills/presentation/viewmodel/SKILL.md
        when: ViewModel, State, UiState, Intent, SideEffect, Orbit MVI

    -   name: presentation/compose
        path: .skills/presentation/compose/SKILL.md
        when: Composable, UiScreen, Content, Preview, koinViewModel, collect state
```
