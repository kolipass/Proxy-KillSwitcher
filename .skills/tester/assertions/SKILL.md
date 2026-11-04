---
name: tester/assertions
description: Assertion selection and verification best practices.
---

```yaml
rules:

    -   id: A01
        rule: "Use assertTrue / assertFalse for boolean assertions"
        do: "assertTrue(actual)"
        dont: "assertEquals(true, actual)"

    -   id: A02
        rule: "Pass expected as first argument to assertEquals"
        do: "assertEquals(expectedState, actual)"
        dont: "assertEquals(actual, expectedState)"

    -   id: A03
        rule: "Use assertNull for null checks"
        do: "assertNull(actual)"
        dont: "assertEquals(null, actual)"

    -   id: A04
        rule: "Use assertNotNull for not-null checks"
        do: "assertNotNull(actual)"
        dont: "assertTrue(actual != null)"

    -   id: A05
        rule: "Use assertInstanceOf<T> for type assertions"
        do: "assertInstanceOf<LoadingState.Success>(actual)"
        dont: "assertTrue(actual is LoadingState.Success)"

    -   id: A06
        rule: "Use assertThrows<E> for exception assertions"
        do: "assertThrows<IllegalStateException> { sut.connect() }"
        dont: "try { sut.connect(); fail() } catch (e: Exception) { }"

    -   id: A07
        rule: "Do not use assertTrue(actual == expected)"
        do: "assertEquals(expected, actual)"
        dont: "assertTrue(actual == expected)"
```
