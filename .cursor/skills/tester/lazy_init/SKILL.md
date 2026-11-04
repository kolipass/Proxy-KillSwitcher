---
name: tester/lazy_init
description: Lazy initialization of the system under test.
---

```yaml
rules:

    -   id: LZ01
        rule: "Use by lazy when sut depends on setUp stubs"
        do: |
            private val sut by lazy {
                ExampleUseCase(scope, navigationProvider, statusUseCase, ...)
            }
        details: "by lazy executes after @BeforeEach — stubs are configured before sut is created"

    -   id: LZ02
        rule: "Use by lazy for sut, not lateinit var"
        do: "private val sut by lazy { FooClass(dependency) }"
        dont: |
            private lateinit var sut: FooClass
            @BeforeEach fun setUp() { sut = FooClass(dependency) }

    -   id: LZ03
        rule: "Initialize sut directly when setUp stubs are not needed"
        do: "private val sut = ExampleAvailabilityConfig(featureToggle, remoteConfigLoader)"
        dont: "private val sut by lazy { ExampleAvailabilityConfig(featureToggle, remoteConfigLoader) }"

    -   id: LZ04
        rule: "Use by lazy only for sut, not for mock fields"
        do: "private val fooRepository: FooRepository = mockk()"
        dont: "private val fooRepository: FooRepository by lazy { mockk() }"

    -   id: LZ05
        rule: "Declare mocks as val, not lateinit var"
        do: "private val fooRepository: FooRepository = mockk()"
        dont: "private lateinit var fooRepository: FooRepository"
```
