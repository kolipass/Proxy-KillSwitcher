---
name: datasource/repository
description: Repository patterns — Store-backed StateFlow, Pref-backed sync, api/impl split.
---

```yaml
rules:

    -   id: R01
        rule: "Constructor accepts only data-layer dependencies — Loader, Storage, Pref, Repository"
        do: |
            class ExampleRepository(
                private val exampleLoader: ExampleLoader,
                private val exampleStorage: ExampleStorage,
                private val examplePrefs: ExamplePrefs,
                private val otherRepository: OtherRepository,
            )
        details: forbidden — AvailabilityConfig, AvailabilityUseCase, UseCase, Observer, Preloader (domain layer)
```
