---
name: presentation/viewmodel
description: Orbit MVI ViewModel, State, UiState, Intent, SideEffect, UiStateMapper patterns.
---

```yaml
rules:

    -   id: VM01
        rule: "Add @Stable to ViewModel class"
        do: |
            @Stable
            @Inject
            class ExampleViewModel
            constructor(...) : ViewModel()

    -   id: VM02
        rule: "Declare uiStateMapper as val (public)"
        do: |
            @Stable
            @Inject
            class ExampleViewModel
            constructor(
                    val uiStateMapper: ExampleUiStateMapper,
                    ...
            ) : ViewModel()

    -   id: VM03
        rule: "No SideEffects: declare ContainerHost with Nothing"
        do: |
            @Stable
            @Inject
            class ExampleViewModel
            constructor(...) : ViewModel(), ContainerHost<ExampleState, Nothing>

    -   id: VM04
        rule: "With SideEffects: declare ContainerHost with the named SideEffect type"
        do: |
            @Stable
            @Inject
            class ExampleViewModel
            constructor(...) : ViewModel(), ContainerHost<ExampleState, ExampleSideEffect>

    -   id: VM05
        rule: "Start Flow subscriptions in container init lambda"
        do: |
            override val container = container<ExampleState, Nothing>(getInitialState()) {
                subscribeToExample()
            }

    -   id: VM06
        rule: "Use reduceState for sync fire-and-forget updates"
        do: |
            fun onAnimationFinished() {
                reduceState { copy(isAnimationVisible = false, isTrainingStarted = true) }
            }
        dont: |
            fun onAnimationFinished() {
                intent { reduce { state.copy(isAnimationVisible = false) } }
            }

    -   id: VM07
        rule: "Use intent { reduce } after async work"
        do: |
            private fun subscribeToExample() {
                intent {
                    exampleUseCase.exampleFlow.collect { example ->
                        reduce { state.copy(example = example) }
                    }
                }
            }
        dont: |
            private fun subscribeToExample() {
                reduceState { copy(example = exampleUseCase.exampleFlow.first()) }
            }

    -   id: VM08
        rule: "Intent is sealed interface with data class/object"
        do: |
            sealed interface ExampleIntent {
                data class ForceStop(val finalTime: Int) : ExampleIntent
                data object JoinClick : ExampleIntent
            }

    -   id: VM09
        rule: "Declare SideEffect as sealed interface"
        do: |
            sealed interface ExampleSideEffect {
                data object RequestSystemPermission : ExampleSideEffect
                data class ShowError(val message: String) : ExampleSideEffect
            }
        dont: "sealed class ExampleSideEffect"

    -   id: VM10
        rule: "State holds domain types only"
        do: |
            data class ExampleState(
                val example: Example,
                val isLoading: Boolean = false,
            )
        dont: |
            data class ExampleState(
                val exampleText: String,
            )

    -   id: VM11
        rule: "@Immutable UiState holds display-ready types only"
        do: |
            @Immutable
            data class ExampleUiState(
                val levelText: String,
                val isButtonVisible: Boolean,
                @StringRes val titleTextRes: Int,
            )
        dont: |
            data class ExampleUiState(
                val example: Example,
            )

    -   id: VM12
        rule: "@Stable UiStateMapper with single map(state) method"
        do: |
            @Stable
            class ExampleUiStateMapper {
                fun map(state: ExampleState): ExampleUiState { ... }
            }
        dont: |
            class ExampleUiStateMapper
            fun mapState(state: ExampleState): ExampleUiState

    -   id: VM13
        rule: "Inject UseCase, UiStateMapper, TimeProvider, Navigator only"
        do: |
            class ExampleViewModel(
                val uiStateMapper: ExampleUiStateMapper,
                private val exampleUseCase: ExampleUseCase,
                private val timeProvider: TimeProvider,
                private val navigator: Navigator,
            )
        dont: |
            class ExampleViewModel(
                private val exampleRepository: ExampleRepository,
                private val prefs: ExamplePreferences,
            )

    -   id: VM14
        rule: "Forbidden: Repository, DataStorage, Preferences, PrefStore"
        do: "Inject UseCase — it wraps Repository access"
        dont: "class ExampleViewModel(private val exampleRepository: ExampleRepository)"
```
