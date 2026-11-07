package mobi.tarantino.proxykillswitcher.feature.update.data

import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateClock

@Inject
class SystemUpdateClock : UpdateClock {
    override fun nowMillis(): Long {
        return System.currentTimeMillis()
    }
}
