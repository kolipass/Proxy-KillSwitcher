---
name: tester/parameterized
description: Parameterized test setup with @MethodSource.
---

```yaml
rules:

    -   id: PT01
        rule: "Use @ParameterizedTest with @MethodSource"
        do: |
            @ParameterizedTest
            @MethodSource("getWithFirstSuffixEvents")
            fun `When event received, Then non-resettable event is sent`(eventType: EventType) { ... }

    -   id: PT02
        rule: "Use List<Arguments> as parameter provider"
        do: |
            @JvmStatic
            fun exampleStatusTestData(): List<Arguments?> {
              return listOf(
                  Arguments.of(ExampleStatus.REQUESTED, Badge.Attention),
                  Arguments.of(ExampleStatus.NOT_REQUESTED, null),
                  Arguments.of(null, null),
              )
            }
        dont: "fun exampleStatusTestData(): Stream<Arguments?>"
        # No concrete example yet in this project — create the first *UiStateMapperTest that needs
        # parameterization, then point this file_reference at it.

    -   id: PT03
        rule: "Name parameter providers 'get{WhatItProvides}'"
        do: "fun getWithFirstSuffixEvents(): List<Arguments>"
        dont: "fun provideEvents(): Stream<Arguments> / fun events(): Stream<Arguments>"

    -   id: PT04
        rule: "Declare provider data as a val list in companion object"
        do: |
            private val WITH_FIRST_SUFFIX_EVENTS = listOf(
                EventType.APP_LAUNCH,
                EventType.APP_SIGN_UP,
            )
        dont: "// compute values directly in provider"
```
