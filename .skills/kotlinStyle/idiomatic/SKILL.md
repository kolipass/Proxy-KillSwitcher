---
name: kotlinStyle/idiomatic
description: Idiomatic Kotlin — prefer the simpler construct over the verbose one.
---

```yaml
rules:

    -   id: ID01
        rule: "Init property directly, not via get()"
        details: "Custom get() recomputes on every access; use only when a fresh value is required"
        do: |
            val isAvailableFlow: Flow<Boolean> = combine(a, b, transform = ::map)
        dont: |
            val isAvailableFlow: Flow<Boolean>
                get() = combine(a, b, transform = ::map)  # rebuilt on every access

    -   id: ID02
        rule: "Method reference over trivial lambda"
        do: |
            .filter(::isOpen)
            .map(Event::itemData)
        dont: |
            .filter { item -> isOpen(item) }

    -   id: ID03
        rule: "Extract non-trivial lambda into named function"
        do: |
            combine(exampleFlowA, exampleFlowB, transform = ::mapExampleState)
            private fun mapExampleState(state: ExampleState, isEnded: Boolean): ExampleState {
                return if (state is ExampleState.Special && isEnded) ExampleState.Standard else state
            }
        dont: |
            combine(exampleFlowA, exampleFlowB) { state, isEnded ->
                if (state is ExampleState.Special && isEnded) ExampleState.Standard else state
            }

    -   id: ID04
        rule: "Function body: block with explicit return"
        do: |
            suspend fun getExample(): Example? {
                return getValue(EXAMPLE_KEY)
            }
        dont: |
            suspend fun getExample(): Example? = getValue(EXAMPLE_KEY)
```
