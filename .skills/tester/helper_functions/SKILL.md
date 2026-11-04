---
name: tester/helper_functions
description: Private helper methods for grouping stubs.
---

```yaml
rules:

    -   id: H01
        rule: "Declare helpers as private methods of the class"
        do: "private fun enableAllConditions() { ... }"
        dont: "fun enableAllConditions() { ... }"

    -   id: H02
        rule: "Create a helper only when 3 or more stubs are grouped"
        do: "every { config.isEnabled } returns true"
        dont: "private fun mockConfig() { every { config.isEnabled } returns true }"

    -   id: H03
        rule: "Use enableAllConditions for grouped stubs"
        do: |
            private fun enableAllConditions() {
                every { config.isExampleFeatureEnabled() } returns true
                every { remoteConfigLoader.exampleFeatureRemotelyEnabled } returns true
                every { repository.exampleData } returns DEFAULT_EXAMPLE_DATA
                every { exampleUseCase.isEligible } returns true
            }

    -   id: H04
        rule: "Place helpers after all test methods"
```
