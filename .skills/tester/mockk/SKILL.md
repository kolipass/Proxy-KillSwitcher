---
name: tester/mockk
description: MockK stubbing and verification patterns.
---

```yaml
rules:

    -   id: M01
        rule: "Use mockk() without relaxed = true"
        do: "private val fooRepository: FooRepository = mockk()"
        dont: "private val fooRepository: FooRepository = mockk(relaxed = true)"

    -   id: M02
        rule: "Use 'just runs' (lowercase) for void functions"
        do: |
            every { settingsLauncher.open() } just runs
            coEvery { storagesRepository.migrate(any()) } just runs
        dont: |
            every { settingsLauncher.open() } just Runs
            coEvery { storagesRepository.migrate(any()) } returns Unit

    -   id: M03
        rule: "Use any() matcher when argument is not verified"
        do: "every { repository.addListener(any()) } just runs"
        dont: "every { repository.addListener(specificListener) } just runs"

    -   id: M04
        rule: "Use coEvery and coVerify for suspend functions"
        do: |
            coEvery { loader.loadData() } returns data
            coVerify(exactly = 1) { loader.loadData() }
        dont: |
            every { loader.loadData() } returns data
            verify(exactly = 1) { loader.loadData() }

    -   id: M05
        rule: "Use coVerify(exactly = 0) instead of inverse = true"
        do: "coVerify(exactly = 0) { dependency.method() }"
        dont: "coVerify(inverse = true) { dependency.method() }"

    -   id: M06
        rule: "Use verify(exactly = 0) instead of inverse = true"
        do: "verify(exactly = 0) { dependency.method() }"
        dont: "verify(inverse = true) { dependency.method() }"

    -   id: M07
        rule: "Use slot to capture callback arguments"
        do: |
            val listenerSlot = slot<PhoenixSocketClientListener>()
            every { client.setListener(capture(listenerSlot)) } just runs
            listenerSlot.captured.onEvent(response)

    -   id: M08
        rule: "AfterEach is required when using mockkObject"
        do: |
            @AfterEach fun tearDown() { unmockkAll() }
            mockkObject(SomeObject)

    -   id: M09
        rule: "Use mockk with block for immediate stubbing"
        do: |
            private val exampleLoader: ExampleLoader = mockk {
                coEvery { loadExample() } just runs
            }

    -   id: M10
        rule: "Use withMockedLogger for tests involving Log"
        do: |
            withMockedLogger<ExampleError> {
                assertFalse(sut.isExampleEnabledFlow.first())
                verify(exactly = 1) { Log.printException(any<Throwable>()) }
            }
        # withMockedLogger doesn't exist yet in this project — add it as a test-source extension
        # (e.g. core/common/src/test/.../LogExt.kt) the first time a test needs to assert on Log.
```
