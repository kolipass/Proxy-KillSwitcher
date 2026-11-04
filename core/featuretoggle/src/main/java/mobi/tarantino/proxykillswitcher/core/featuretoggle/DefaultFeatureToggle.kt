package mobi.tarantino.proxykillswitcher.core.featuretoggle

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultFeatureToggle
    @Inject
    constructor() : FeatureToggle
