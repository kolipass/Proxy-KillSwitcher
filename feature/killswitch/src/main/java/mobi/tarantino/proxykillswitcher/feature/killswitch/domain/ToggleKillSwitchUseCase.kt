package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository

@Inject
class ToggleKillSwitchUseCase
    constructor(
        private val killSwitchRepository: KillSwitchRepository,
    ) {
        operator fun invoke(desiredEnabled: Boolean) {
            killSwitchRepository.setDesiredEnabled(desiredEnabled)
        }
    }
