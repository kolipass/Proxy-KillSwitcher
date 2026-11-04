---
name: domain
description: Orchestrator for the domain layer — creates and edits UseCase, AvailabilityConfig, AvailabilityUseCase and PreLoader classes.
---

```yaml
sub_skills:
  - name: domain/usecase
    path: .cursor/skills/domain/usecase/SKILL.md
    when: UseCase, business logic

  - name: domain/availabilityConfig
    path: .cursor/skills/domain/availabilityConfig/SKILL.md
    when: AvailabilityConfig, FeatureToggle, RemoteConfig

  - name: domain/availabilityUsecase
    path: .cursor/skills/domain/availabilityUsecase/SKILL.md
    when: AvailabilityUseCase, isExampleAvailable, final feature availability

  - name: domain/preloader
    path: .cursor/skills/domain/preloader/SKILL.md
    when: PreLoader, preload, warm cache, early data fetch before UseCase
```
