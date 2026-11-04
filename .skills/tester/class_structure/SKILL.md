---
name: tester/class_structure
description: Test class field order, blank lines, and method placement.
---

```yaml
rules:

    -   id: CS01
        rule: "Declare fields in order: testScope, mocks, sut"
        do: |
            private val testScope = TestScope(StandardTestDispatcher())
            private val fooRepository: FooRepository = mockk()

            private val sut = FooUseCase(fooRepository)
        dont: |
            private val sut = FooUseCase(fooRepository)
            private val fooRepository: FooRepository = mockk()

    -   id: CS02
        rule: "Add a blank line before sut"
        do: |
            private val fooRepository: FooRepository = mockk()

            private val sut = FooUseCase(fooRepository)
        dont: |
            private val fooRepository: FooRepository = mockk()
            private val sut = FooUseCase(fooRepository)

    -   id: CS03
        rule: "Add a blank line after sut"
        do: |
            private val sut = FooUseCase(fooRepository)

            @BeforeEach
            fun setUp() { ... }

    -   id: CS04
        rule: "No blank lines between mock field declarations"
        do: |
            private val fooRepository: FooRepository = mockk()
            private val barService: BarService = mockk()
        dont: |
            private val fooRepository: FooRepository = mockk()

            private val barService: BarService = mockk()

    -   id: CS05
        rule: "Place companion object at end of class"
        do: |
            @Test fun `When ...`() { }

            companion object { ... }

    -   id: CS06
        rule: "Place helper functions before companion object"
        do: |
            @Test fun `When ...`() { }

            private fun enableAllConditions() { }

            companion object { }

    -   id: CS07
        rule: "Place BeforeEach and AfterEach before test methods"
        do: |
            @BeforeEach fun setUp() { }
            @Test fun `When ...`() { }
        dont: |
            @Test fun `When ...`() { }
            @AfterEach fun tearDown() { }

    -   id: CS08
        rule: "Add a blank line after class opening brace"
        do: |
            class FooTest {

                private val sut = Foo()
        dont: |
            class FooTest {
                private val sut = Foo()

    -   id: CS09
        rule: "Declare nested data types as private"
        do: "private data class TestValue(override val expiresAt: Long) : ExpiringValue"
        dont: "data class TestValue(override val expiresAt: Long) : ExpiringValue"
```
