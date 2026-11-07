package mobi.tarantino.proxykillswitcher.feature.update

import kotlinx.coroutines.test.TestScope

fun TestScope.advanceUntilIdleAll() {
    testScheduler.runCurrent()
}
