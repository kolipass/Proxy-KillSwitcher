---
name: diMetro
description: Metro DI — constructor injection, scoping, binding, multibindings, binding containers.
---

```yaml
rules:

    -   id: MT01
        rule: "New class — @Inject, never @Provides"
        do: |
            @Inject
            class ExamplePrefs
            constructor(...)
        dont: |
            @Provides
            @SingleIn(AppScope::class)
            fun provideExamplePrefs() = ExamplePrefs()

    -   id: MT02
        rule: "@SingleIn on the class declares scope"
        do: |
            @SingleIn(ExampleScope::class)
            @Inject
            class ExampleRepository

    -   id: MT03
        rule: "@Provides only for foreign types"
        details: "types whose constructor cannot be annotated — Gson, Context, third-party"
        do: |
            @Provides
            @SingleIn(AppScope::class)
            fun provideGson(): Gson = GsonBuilder().create()

    -   id: MT04
        rule: "Multibinding — @Binds @IntoSet in BindingContainer"
        do: |
            @Binds
            @IntoSet
            abstract fun bindExamplePrefs(prefs: ExamplePrefs): Preferences
```
