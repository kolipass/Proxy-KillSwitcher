---
name: presentation/compose
description: Jetpack Compose UiScreen, Content, Preview, and PreviewData patterns.
---

```yaml
rules:

    -   id: CP01
        rule: "UiScreen calls Content — two-level structure"
        do: |
            @Composable
            fun ExampleUiScreen() {
                val viewModel = viewModel<ExampleViewModel>()
                val state by viewModel.collectAsState()
                ExampleContent(
                    uiState = viewModel.uiStateMapper.map(state),
                    onIntent = viewModel::onIntent,
                )
            }

            @Composable
            private fun ExampleContent(uiState: ExampleUiState, onIntent: (ExampleIntent) -> Unit) { ... }
        dont: |
            @Composable
            fun ExampleUiScreen() {
                val viewModel = viewModel<ExampleViewModel>()
                // UI code directly in UiScreen — Content layer is missing
            }

    -   id: CP02
        rule: "Get ViewModel via viewModel<T>()"
        do:   "val viewModel = viewModel<ExampleViewModel>()"
        dont: |
            val viewModel = hiltViewModel<ExampleViewModel>()
            val viewModel: ExampleViewModel by inject()

    -   id: CP03
        rule: "Collect Orbit state via collectAsState()"
        do:   "val state by viewModel.collectAsState()"

    -   id: CP04
        rule: "Map state only in root UiScreen"
        do: |
            fun ExampleUiScreen() {
                val state by viewModel.collectAsState()
                ExampleContent(uiState = viewModel.uiStateMapper.map(state), ...)
            }

    -   id: CP05
        rule: "Pass onIntent lambda, never ViewModel itself"
        do: |
            ExampleContent(
                uiState = ...,
                onIntent = viewModel::onIntent,
            )

            @Composable
            private fun ExampleContent(uiState: ExampleUiState, onIntent: (ExampleIntent) -> Unit)
        dont: |
            ExampleContent(viewModel = viewModel)  # ViewModel must not leave UiScreen

    -   id: CP06
        rule: "Use collectSideEffect only when SideEffects exist"
        do: |
            viewModel.collectSideEffect { sideEffect ->
                when (sideEffect) {
                    ExampleSideEffect.RequestPermission -> launcher.launch(permission)
                }
            }

    -   id: CP07
        rule: "Decompose Content into private Composable functions"
        do: |
            @Composable
            private fun ExampleContent(...) {
                ExampleHeader(...)
                ExampleBody(...)
            }

            @Composable
            private fun ExampleHeader(...)

            @Composable
            private fun ExampleBody(...)
        dont: |
            @Composable
            private fun ExampleContent(...) {
                Column {
                    // 200 lines of UI in a single function
                }
            }

    -   id: CP08
        rule: "@Preview with AppTheme for each Content composable"
        do: |
            @Preview(showBackground = true)
            @Composable
            private fun ExampleContentPreview() {
                AppTheme {
                    ExampleContent(
                        uiState = ExampleContentPreviewData.uiState,
                        onIntent = {},
                    )
                }
            }

    -   id: CP09
        rule: "Create PreviewData object with ready-to-use UiState"
        do: |
            object ExampleContentPreviewData {
                val uiState = ExampleUiState(
                    displayValue = "Preview",
                    isButtonVisible = true,
                )
            }
        details: never inline a stub UiState in the Preview call — always reference PreviewData

    -   id: CP10
        rule: "Drive UI timers via LaunchedEffect + delay, not ViewModel"
        do: |
            LaunchedEffect(isTimerActive) {
                if (isTimerActive) {
                    while (true) {
                        delay(1000L)
                        onIntent(ExampleIntent.Tick)
                    }
                }
            }

    -   id: CP11
        rule: "Package must end in .{feature}.ui"
        do:   "package com.example.app.feature.progress.ui"
        dont: "package com.example.app.feature.progress  # ui sub-package is required"

    -   id: CP12
        rule: "No business logic in Composables"
        do:   "Pure UI: layout, state display, event forwarding via onIntent"
        dont: |
            @Composable
            fun ExampleContent(...) {
                val percent = (uiState.done.toFloat() / uiState.total) * 100  # business logic in Composable
            }

    -   id: CP13
        rule: "@Preview function lives in the same file as its composable"
        do: |
            // main/…/ExampleButton.kt
            @Composable
            fun ExampleButton(...) { ... }

            @Preview(showBackground = true)
            @Composable
            private fun ExampleButtonPreview(...) { ... }

    -   id: CP14
        rule: "Take dimensions and palette tokens from proxy killswitch.pen"
        do: |
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 56.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            )
```
