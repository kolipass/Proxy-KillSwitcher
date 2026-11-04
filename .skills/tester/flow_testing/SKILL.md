---
name: tester/flow_testing
description: Flow and StateFlow testing patterns.
---

```yaml
rules:

    -   id: FL01
        rule: "Use flowOf() to mock a Flow with a single value"
        do: "every { repository.dataFlow } returns flowOf(DEFAULT_DATA)"
        dont: "every { repository.dataFlow } returns MutableStateFlow(DEFAULT_DATA)"

    -   id: FL02
        rule: "Use flow.first() to get the first emitted value"
        do: "val actual = sut.isAvailableFlow.first()"
        dont: |
            var actual: Boolean = false
            sut.isAvailableFlow.take(1).collect { isAvailable -> actual = isAvailable }

    -   id: FL03
        rule: "Use stateFlow.value to assert StateFlow content"
        do: "assertEquals(expected, repository.stateFlow.value)"

    -   id: FL04
        rule: "Collect a Flow inside launch for subscription"
        do: |
            val job = launch { flow.collect { results += it } }
            advanceUntilIdle()
            job.cancel()

    -   id: FL05
        rule: "Use flowOf(null) for Flow with a null value"
        do: "every { repository.exampleDataFlow } returns flowOf(null)"
        dont: "every { repository.exampleDataFlow } returns flowOf<Data?>(null)"

    -   id: FL06
        rule: "Mock Flow using sut initialized by lazy"
        do: |
            private val sut by lazy {
                UseCase(dependency, flowDependency)
            }

            @Test
            fun `When condition, Then result`() {
                every { flowDependency.flow } returns flowOf(true)

                val result = sut.execute()

                assertTrue(result)
            }
        dont: |
            private val sut = UseCase(dependency, flowDependency)

            @Test
            fun `When condition, Then result`() {
                every { flowDependency.flow } returns flowOf(true)
                val result = sut.execute()
                assertTrue(result)
            }
```
