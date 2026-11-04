---
name: kotlinStyle
description: Orchestrator — Kotlin code style for data classes, idiomatic syntax, lambdas, self-documenting code and Flow.
---

```yaml
rule: Read every sub-skill whose `when` matches and follow its rules.
sub_skills:
    -   name: kotlinStyle/dataClass
        path: .cursor/skills/kotlinStyle/dataClass/SKILL.md
        when: data class, DTO, model type, nesting, top-level type placement

    -   name: kotlinStyle/idiomatic
        path: .cursor/skills/kotlinStyle/idiomatic/SKILL.md
        when: property declaration, custom getter, get(), expression body, idiomatic Kotlin

    -   name: kotlinStyle/lambda
        path: .cursor/skills/kotlinStyle/lambda/SKILL.md
        when: lambda parameters, trailing lambda, SAM conversion, callback

    -   name: kotlinStyle/selfDocumenting
        path: .cursor/skills/kotlinStyle/selfDocumenting/SKILL.md
        when: writing or editing any Kotlin code

    -   name: kotlinStyle/flow
        path: .cursor/skills/kotlinStyle/flow/SKILL.md
        when: nullable Flow, filterNotNull, exposing a shared Flow to consumers
```
