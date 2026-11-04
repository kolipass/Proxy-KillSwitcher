---
name: domain/availabilityUsecase
description: AvailabilityUseCase patterns — final per-user availability over AvailabilityConfig, Flow exposure, naming.
---

```yaml
rules:

    -   id: AU01
        rule: "Always name class <FeatureName>AvailabilityUseCase"
        do: |
            class MarketAvailabilityUseCase(...)  # <FeatureName> = Market
        dont: |
            class MarketUseCase(...)  # missing "Availability" — breaks the naming contract
        details: one class per feature — final per-user availability, combines Config with business state

    -   id: AU02
        rule: "Combine Config with dynamic business conditions"
        do: |
            private fun isExampleAvailable(profile: Profile?): Boolean {
                return exampleAvailabilityConfig.isExampleAvailable && profile?.isEligible == true
            }

    -   id: AU03
        rule: "Flow name equals field name plus Flow"
        do: |
            val isExampleAvailableFlow: Flow<Boolean>
            val isExampleAvailable: Boolean
            private fun isExampleAvailable(profile: Profile?): Boolean
        details: field, Flow suffix, and delegate method all share one name

    -   id: AU04
        rule: "Flow requires a private delegate method"
        do: |
            val isExampleAvailableFlow: Flow<Boolean> = profileRepository.profileFlow
                .map(::isExampleAvailable)
            val isExampleAvailable: Boolean
                get() = isExampleAvailable(profileRepository.profile)
            private fun isExampleAvailable(profile: Profile?): Boolean {
                return exampleAvailabilityConfig.isExampleAvailable && profile?.isEligible == true
            }

    -   id: AU05
        rule: "Always split UseCase by availability kind"
        do: |
            class ExampleAvailabilityUseCase(...)       # general feature availability
            class ExamplePromoAvailabilityUseCase(...)  # narrower — promo-only conditions
        details: multiple availability kinds require a new, narrower AvailabilityUseCase
```
