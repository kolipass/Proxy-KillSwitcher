package mobi.tarantino.proxykillswitcher.feature.killswitch

import kotlinx.coroutines.test.TestScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.test.test

suspend fun <STATE : Any, SIDE_EFFECT : Any> ContainerHost<STATE, SIDE_EFFECT>.runActionAndCheckResultState(
    testScope: TestScope,
    initialState: STATE,
    expectedState: STATE,
    action: () -> Unit,
) {
    test(testScope = testScope, initialState = initialState) {
        val onCreateJob = runOnCreate()
        action()
        testScope.advanceUntilIdleAll()
        expectState { expectedState }
        cancelAndIgnoreRemainingItems()
        onCreateJob.cancel()
        testScope.advanceUntilIdleAll()
    }
}

fun TestScope.advanceUntilIdleAll() {
    testScheduler.runCurrent()
}
