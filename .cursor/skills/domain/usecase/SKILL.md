---
name: domain/usecase
description: UseCase business logic patterns — Flow exposure, combine, method references, API/impl split.
---

```yaml
rules:

    -   id: UC01
        rule: "Constructor accepts only Repository and domain-layer dependencies"
        do: |
            class ExampleUseCase
            constructor(
                private val scope: CoroutineScope,
                private val otherUseCase: OtherUseCase,
                private val timeProvider: TimeProvider,
                private val exampleRepository: ExampleRepository,
                private val exampleAvailabilityUseCase: ExampleAvailabilityUseCase,
            )
        details: forbidden — Loader, Storage, Preferences, Observer, Preloader, AvailabilityConfig.

    -   id: UC02
        rule: "Check availability only via AvailabilityUseCase"
        do: |
            class ExampleUseCase
            constructor(
                private val exampleAvailabilityUseCase: ExampleAvailabilityUseCase,
            ) {
                val isAvailableFlow: Flow<Boolean> = exampleAvailabilityUseCase.isAvailableFlow

                val isAvailable: Boolean
                    get() = exampleAvailabilityUseCase.isAvailable
            }

    -   id: UC03
        rule: "Put constants in companion object"
        do: |
            companion object {
                const val EXAMPLE_DELAY_MS = 1_000L
                private val EXAMPLE_INTERVAL_MS = TimeUnit.HOURS.toMillis(1)
            }
```
