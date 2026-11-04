---
name: tester/coroutines
description: Coroutine testing with TestDispatcher and TestScope.
---

```yaml
rules:

    -   id: CR01
        rule: "Use verifyOrder for relative order verification of regular calls"
        do: |
            verifyOrder {
                mapper.mapExampleData()
                interactor.addItem(item)
            }

    -   id: CR02
        rule: "Use verifySequence for strict order verification of regular calls"
        do: |
            verifySequence {
                mapper.mapFirstDepositPopupData()
                interactor.addPopup(popupData)
            }
        dont: |
            verify { mapper.mapFirstDepositPopupData() }
            verify { interactor.addPopup(popupData) }

    -   id: CR03
        rule: "Use coVerifyOrder for suspend call order verification"
        do: |
            coVerifyOrder {
                preferencesRepository.migrate(OLD_VERSION)
                migrationPrefs.settingsVersion = latestVersion
            }

    -   id: CR04
        rule: "Use suspend test function for coroutine tests instead of runTest"
        do: "@Test suspend fun `When action, Then result`() { sut.load(); verify { ... } }"
        dont: "@Test fun `When action, Then result`() = runTest { sut.load(); verify { ... } }"

    -   id: CR05
        rule: "Use StandardTestDispatcher for most tests"
        do: "private val dispatcher = StandardTestDispatcher()"
        dont: "private val dispatcher = UnconfinedTestDispatcher()"

    -   id: CR06
        rule: "Create TestScope from StandardTestDispatcher"
        do: "private val testScope = TestScope(StandardTestDispatcher())"
        dont: |
            private val testScope = TestScope()
            private val testScope = CoroutineScope()
            private val scope = CoroutineScope()

    -   id: CR07
        rule: "Use UnconfinedTestDispatcher for immediate execution"
        do: "private val testScope = TestScope(UnconfinedTestDispatcher())"
        dont: |
            private val testScope = TestScope(StandardTestDispatcher())
            private val testScope = CoroutineScope()
            private val scope = CoroutineScope()

    -   id: CR08
        rule: "Use testScope.advanceUntilIdleAll() instead of advanceUntilIdle()"
        do: "testScope.advanceUntilIdleAll()"
        dont: "testScheduler.advanceUntilIdle()"

    -   id: CR09
        rule: "Call testScope.cancel() in tearDown"
        do: |
            @AfterEach fun tearDown() {
                testScope.cancel()
            }
        dont: "testScope.coroutineContext.cancelChildren()"

    -   id: CR10
        rule: "Prohibit Dispatchers.setMain usage in tests"
        do: "private val testScope = TestScope(UnconfinedTestDispatcher())"
        dont: |
            Dispatchers.setMain(testDispatcher)

            @AfterEach
            fun tearDown() {
                Dispatchers.resetMain()
            }

    -   id: CR11
        rule: "Do not use runBlocking in tests"
        do: "@Test suspend fun `When action, Then result`() { val actual = sut.load() }"
        dont: "@Test fun `When action, Then result`() = runBlocking { val actual = sut.load() }"

    -   id: CR12
        rule: "Wrap action in testScope.runTest() when it uses another dispatcher"
        do: |
            testScope.runTest { sut.sendMetric(LABEL) }
            coVerify { mapper.mapMetric(LABEL, null, null) }
```
