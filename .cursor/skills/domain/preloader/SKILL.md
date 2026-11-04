---
name: domain/preloader
description: PreLoader patterns — warming caches and early data loads ahead of a UseCase.
---

```yaml
rules:

    -   id: PR01
        rule: "Mainly depend on AvailabilityUseCase, not Config"
        do: |
            class ExamplePreLoader(
                private val exampleUseCase: ExampleUseCase,
                private val exampleAvailabilityUseCase: ExampleAvailabilityUseCase,
            )
        details: AvailabilityConfig allowed only as a documented preload exception
```
