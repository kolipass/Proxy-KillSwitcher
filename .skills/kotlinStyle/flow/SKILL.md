---
name: kotlinStyle/flow
description: Flow exposure conventions.
---

```yaml
rules:

    -   id: FW01
        rule: "Filter Flow in consumer, not producer"
        details: "Producer forcing filterNotNull() hides null state from every consumer, even ones that need it"
        do: |
            val itemsFlow: Flow<List<Item>?> = exampleRepository.itemsFlow
        dont: |
            val itemsFlow: Flow<List<Item>> = exampleRepository.itemsFlow.filterNotNull()
```
