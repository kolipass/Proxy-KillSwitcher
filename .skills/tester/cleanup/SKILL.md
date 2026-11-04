---
name: tester/cleanup
description: Mock cleanup and tearDown ordering.
---

```yaml
rules:

    -   id: CL01
        rule: "Use unmockkAll for full mock cleanup in tests"
        do: "@AfterEach fun tearDown() { unmockkAll() }"
        dont: "unmockkObject(Foo); unmockkObject(Bar)"

    -   id: CL02
        rule: "Order tearDown: custom logic first, then unmockkAll"
        do: |
            fun tearDown() {
                testScope.coroutineContext.cancelChildren()
                unmockkAll()
            }
        dont: |
            fun tearDown() {
                unmockkAll()
                testScope.coroutineContext.cancelChildren()
            }
```
