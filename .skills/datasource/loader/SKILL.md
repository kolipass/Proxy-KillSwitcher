---
name: datasource/loader
description: Remote Loader and Local Loader classes — fetch data over network or read it from local assets/cache.
---

```yaml
rules:

    -   id: LD01
        rule: "Remote Loader constructor takes only ApiServiceProvider"
        do: |
            class RemoteExampleLoader(
                private val apiServiceProvider: ApiServiceProvider,
            ) : ExampleLoader

    -   id: LD02
        rule: "No mapping, branching or validation in methods"
        do: |
            override suspend fun joinEvent(eventId: String): ExampleEvent {
                return apiServiceProvider.exampleApiService
                    .joinEvent(eventId)
                    .awaitApiCall()
                    .data
            }
```
