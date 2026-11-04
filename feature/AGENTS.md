# :feature:*

Each feature module follows `:feature:killswitch`. Bindings join the app graph with
`@ContributesTo(AppScope::class)` — do not list feature types by hand in `:app`.

Read `.skills/kotlinStyle/SKILL.md` on every Kotlin edit, then the orchestrator for the package you are in:

| Package | Orchestrator |
|---|---|
| `data/` | `.skills/datasource/SKILL.md` |
| `domain/` | `.skills/domain/SKILL.md` |
| `presentation/` | `.skills/presentation/SKILL.md` |
| `di/` | `.skills/diMetro/SKILL.md` |
| `navigation/` | `.skills/navigation/SKILL.md` |

`*Availability*` types: `.skills/domain/SKILL.md` (`availabilityConfig`, `availabilityUsecase`).

`*Test.kt` / `src/test/`: `.skills/tester/SKILL.md`.
