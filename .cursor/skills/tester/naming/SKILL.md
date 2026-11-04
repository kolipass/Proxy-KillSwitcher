---
name: tester/naming
description: Test element naming conventions.
---

```yaml
rules:

    -   id: N01
        rule: "Name the system under test 'sut'"
        do: "private val sut = FooUseCase(repository)"
        dont: "private val fooUseCase = FooUseCase(repository)"

    -   id: N02
        rule: "Name the test result 'actual'"
        do: "val actual = sut.load()"
        dont: "val result = sut.load()"

    -   id: N03
        rule: "Name tests in format: When action, Then result"
        do: "`When action performed, Then result expected`"
        dont: "`should return value when action` / `test loadData`"

    -   id: N04
        rule: "Name the expected value 'expected'"
        do: "val expected = FooState(isLoading = true)"
        dont: "val expectedResult = FooState(isLoading = true)"

    -   id: N05
        rule: "Name the test class TestedClassTest"
        do: "class FooUseCaseTest"
        dont: "class TestFooUseCase / class FooTest"

    -   id: N06
        rule: "Use full type name for mock field declarations"
        do: "private val fooRepository: FooRepository = mockk()"
        dont: "private val repository: FooRepository = mockk()"

    -   id: N07
        rule: "Name the BeforeEach method 'setUp'"
        do: "@BeforeEach fun setUp()"
        dont: "@BeforeEach fun before() / fun init() / fun prepare()"

    -   id: N08
        rule: "Name the AfterEach method 'tearDown'"
        do: "@AfterEach fun tearDown()"
        dont: "@AfterEach fun after() / fun cleanup()"

    -   id: N09
        rule: "Prefix mass-stub helper methods with 'enableAll'"
        do: "private fun enableAllConditions() / enableAllConditionsFlows()"
        dont: "private fun setupMocks() / fun prepareAll()"

    -   id: N10
        rule: "Name the dispatcher field 'dispatcher'"
        do: "private val dispatcher = StandardTestDispatcher()"
        dont: "private val testDispatcher = StandardTestDispatcher()"

    -   id: N11
        rule: "Name the TestScope for advanceUntilIdleAll 'testScope'"
        do: "private val testScope = TestScope(StandardTestDispatcher())"
        dont: "private val scope = TestScope(StandardTestDispatcher())"

    -   id: N12
        rule: "Keep the all test name string at or under 120 characters"
        do: "`When valid, Then state updated`"
        dont: "`When all the preconditions for this particular feature flag combination are satisfied and the user proceeds, Then the resulting state is updated accordingly`"
```
