---
name: tester/orbit
description: Orbit MVI ViewModel testing patterns.
---

```yaml
rules:

    -   id: OM01
        rule: "Test ViewModels through runActionAndCheckResultState"
        do: |
            sut.runActionAndCheckResultState(
                testScope = testScope,
                initialState = INITIAL_STATE,
                expectedState = EXPECTED_STATE,
            ) { sut.loadData() }
        # runActionAndCheckResultState doesn't exist yet in this project — add it as a shared
        # orbit-test extension (e.g. core/common/src/test/.../OrbitTestExt.kt) once the first
        # Orbit ViewModel test needs it.

    -   id: OM02
        rule: "Call runOnCreate inside the orbit-test block"
        do: "test(testScope, initialState) { runOnCreate(); action() }"
        dont: "test(testScope, initialState) { action() }"

    -   id: OM03
        rule: "Pass the expected state block to expectState"
        do: "expectState { FooState(isLoading = false, data = DATA) }"
        dont: "assertEquals(FooState(...), awaitItem())"

    -   id: OM04
        rule: "Use expectNoItems when state does not change"
        do: "expectNoItems()"
        dont: "// no state check — state may have changed"

    -   id: OM05
        rule: "Call cancelAndIgnoreRemainingItems at end of orbit test"
        do: "cancelAndIgnoreRemainingItems()"
```
