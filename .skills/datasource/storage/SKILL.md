---
name: datasource/storage
description: Local persistence layer — Prefs and Storage subclasses over SharedPreferences, DataStore and Protobuf.
---

```yaml
rules:

    -   id: DS01
        rule: "Storage delegates only — no business logic"
        do: |
            suspend fun getExample(): Example? {
                return getValue(EXAMPLE_KEY)
            }

    -   id: DS02
        rule: "Prefs delegates only — no business logic"
        do: |
            var example: String? by property(EXAMPLE_KEY)

    -   id: DS03
        rule: "Pref fields hold only primitives or strings"
        do: |
            class ExamplePrefs : Preferences(NAME) {
                var exampleId: String? by property(EXAMPLE_ID_KEY)
            }

    -   id: DS04
        rule: "Storage methods return only data models"
        do: |
            class ExampleStorage(...) : Storage(...) {
                suspend fun getExample(): Example? {
                    return getValue(EXAMPLE_KEY)
                }
            }
```
