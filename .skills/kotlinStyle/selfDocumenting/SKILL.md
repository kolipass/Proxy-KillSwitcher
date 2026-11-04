---
name: kotlinStyle/selfDocumenting
description: Self-documenting code — intent lives in names, never in comments.
---

```yaml
rules:

    -   id: SD01
        rule: "Names of classes, functions, properties express intent"
        do: |
            val wireName: String
            fun startNextTrainingRound()
        dont: |
            val n: String
            fun next()  # reader can't tell what advances

    -   id: SD02
        rule: "No comments of any kind — line, block, KDoc"
        details: "If a comment feels needed — rename or decompose instead"
        do: |
            interface AnalyticsAction {
                val wireName: String
            }
        dont: |
            /** The only source of the analytics event_name. */  # KDoc duplicates what naming must express
            interface AnalyticsAction { val wireName: String }

    -   id: SD03
        rule: "Map property name states its key"
        do: |
            val totalCountByRegionId: Map<String, Long>
            val itemsByCategory: Map<Category, List<Item>>

    -   id: SD04
        rule: "No typealias aliasing a primitive"
        do: |
            val totalCountByRegionId: Map<String, Long>
            typealias Schedules = Map<ItemType, List<Schedule>>

    -   id: SD05
        rule: "Specific parameter name over generic"
        do: |
            fun filterItems(activeItems: List<ActiveItem>)
        dont: |
            fun filterItems(items: List<ActiveItem>)

    -   id: SD06
        rule: "State prefix for freshly received data"
        details: "updated/current when data just arrived from source"
        do: |
            val updatedItemsData = events.flatMap(Event::itemData)
            val updatedEntriesById: Map<String, Entry>
```
