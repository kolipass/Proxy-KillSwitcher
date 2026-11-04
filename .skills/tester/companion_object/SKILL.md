---
name: tester/companion_object
description: Test constants placement and declaration in companion object.
---

```yaml
rules:

    -   id: CO01
        rule: "Place all test constants in companion object"
        do: |
            companion object {

                private const val OLD_VERSION = 1
            }
        dont: "private val OLD_VERSION = 1"

    -   id: CO02
        rule: "Add a blank line after companion object opening brace"
        do: |
            companion object {

                private const val CURRENT_TIME = 1000000L
        dont: |
            companion object {
                private const val CURRENT_TIME = 1000000L

    -   id: CO03
        rule: "Declare companion object constants as private"
        do: "private const val CURRENT_TIME = 1000000L"
        dont: "const val CURRENT_TIME = 1000000L"

    -   id: CO04
        rule: "Use const val for primitive constants"
        do: "private const val DEAL_AMOUNT = 100000L"
        dont: "private val DEAL_AMOUNT = 100000L"

    -   id: CO05
        rule: "Add explicit type suffix to numeric literals"
        do: "private const val CURRENT_TIME = 1000000L"
        dont: "private const val CURRENT_TIME = 1000000"
```
