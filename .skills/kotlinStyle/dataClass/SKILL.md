---
name: kotlinStyle/dataClass
description: Data class structure — nesting and scope of model types.
---

```yaml
rules:

    -   id: DC01
        rule: "Nest a data class used by a single owner"
        details: placing it top-level instead leaks its scope into the whole file, not just the owner
        do: |
            data class ExampleEvent
            constructor(
                    val status: String,
                    val progress: Progress?,
            ) {

                object Status {

                    const val STARTED = "started"
                    const val COMPLETED = "completed"
                }

                data class Progress
                constructor(
                        val currentLevel: Int,
                )
            }
```
