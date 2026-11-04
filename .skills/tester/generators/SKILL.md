---
name: tester/generators
description: Test data generator objects and constants.
---

```yaml
rules:

    -   id: G01
        rule: "Move lists of 5+ enum values in companion to Generator"
        do: |
            private val WITH_FIRST_SUFFIX_EVENTS = AnalyticsEventTypeGenerator.WITH_FIRST_SUFFIX_EVENTS
        dont: |
            private val WITH_FIRST_SUFFIX_EVENTS = listOf(
                EventType.APP_LAUNCH, EventType.APP_SIGN_UP,
            )
        details: "if the same list is needed in 2+ tests — Generator is mandatory"

    -   id: G02
        rule: "Use Generator for objects with 3 or more fields"
        do: "val item = ItemGenerator.EXAMPLE_ITEM"
        dont: |
            val item = Item(id = ID, name = NAME, category = CATEGORY,
                status = Status.ACTIVE, amount = 100000L, kind = Kind.STANDARD, ...)

    -   id: G03
        rule: "Declare Generator as an object, not a class"
        do: "object ItemGenerator { ... }"
        dont: "class ItemGenerator { ... }"

    -   id: G04
        rule: "Place Generator file in the test package root"
        do: "core/common/src/test/kotlin/mobi/tarantino/proxykillswitcher/ItemGenerator.kt"
        dont: "core/common/src/test/kotlin/mobi/tarantino/proxykillswitcher/items/ItemGenerator.kt"

    -   id: G05
        rule: "Use UPPER_SNAKE_CASE for Generator constants"
        do: "const val ITEM_AMOUNT = 100000L"
        dont: "const val itemAmount = 100000L"

    -   id: G06
        rule: "Generator must contain only fields, no methods"
        do: |
            object ProfileGenerator {
                val FREE_STATUS_PROFILE = ExampleProfile(...)
            }
        dont: |
            object ProfileGenerator {
                fun createFreeProfile(): ExampleProfile { ... }
            }

    -   id: G07
        rule: "Check for existing Generator before creating a new one"
        details: "Check existing Generators before creating a new one"

    -   id: G08
        rule: "Name Generator files EntityNameGenerator.kt"
        do: "ItemGenerator.kt / ProfileGenerator.kt / StatusGenerator.kt"
        dont: "TestData.kt / Fixtures.kt / FakeItem.kt"
```
