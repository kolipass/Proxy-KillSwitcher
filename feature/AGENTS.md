# :feature:*

Each feature module follows `:feature:killswitch`. Bindings join the app graph with
`@ContributesTo(AppScope::class)` — do not list feature types by hand in `:app`.

Read `.cursor/skills/kotlinStyle/SKILL.md` on every Kotlin edit, then the orchestrator for the package you are in:

| Package | Orchestrator |
|---|---|
| `data/` | `.cursor/skills/datasource/SKILL.md` |
| `domain/` | `.cursor/skills/domain/SKILL.md` |
| `presentation/` | `.cursor/skills/presentation/SKILL.md` |
| `di/` | `.cursor/skills/diMetro/SKILL.md` |
| `navigation/` | `.cursor/skills/navigation/SKILL.md` |

`*Availability*` types: `.cursor/skills/domain/SKILL.md` (`availabilityConfig`, `availabilityUsecase`).

`*Test.kt` / `src/test/`: `.cursor/skills/tester/SKILL.md`.
