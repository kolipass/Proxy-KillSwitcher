package mobi.tarantino.proxykillswitcher

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope

@BindingContainer
@ContributesTo(AppScope::class)
interface AppContextModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideContext(application: Application): Context {
            return application
        }
    }
}
