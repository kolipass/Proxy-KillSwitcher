---
name: domain/availabilityConfig
description: AvailabilityConfig patterns — technical feature gating via FeatureToggle, Remote Config and build config.
---

```yaml
rules:

    -   id: AC01
        rule: "Always name class <FeatureName>AvailabilityConfig"
        do: |
            class MarketAvailabilityConfig(...)  # <FeatureName> = Market
        dont: |
            class MarketConfig(...)  # missing "Availability" — breaks the naming contract
        details: one class per feature — technical availability gate, not business logic

    -   id: AC02
        rule: "Combine only technical toggles, never business state"
        do: |
            val isExampleAvailable: Boolean
                get() = featureToggle.isExampleEnabled() && remoteConfigLoader.isExampleEnabled

    -   id: AC03
        rule: "Depend only on classes carrying static info"
        do: |
            class ExampleAvailabilityConfig(
                private val featureToggle: FeatureToggle,
                private val remoteConfigLoader: RemoteConfigLoader,
            )
        details: allowed set today — FeatureToggle, RemoteConfigLoader (core:featuretoggle); extend this list only once a new static-info class actually exists

    -   id: AC04
        rule: "Flow name equals field name plus Flow"
        do: |
            val isExampleAvailableFlow: Flow<Boolean>
            val isExampleAvailable: Boolean
            private fun isExampleAvailable(remoteConfigLoader: RemoteConfigLoader): Boolean
        details: field, Flow suffix, and delegate method all share one name

    -   id: AC05
        rule: "Flow requires a private delegate method"
        do: |
            val isExampleAvailableFlow: Flow<Boolean> = remoteConfigLoader.remoteConfigLoaderFlow
                .map(::isExampleAvailable)
            val isExampleAvailable: Boolean
                get() = isExampleAvailable(remoteConfigLoader)
            private fun isExampleAvailable(remoteConfigLoader: RemoteConfigLoader): Boolean {
                return featureToggle.isExampleEnabled() && remoteConfigLoader.isExampleEnabled
            }
```
