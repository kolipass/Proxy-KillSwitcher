package mobi.tarantino.proxykillswitcher.feature.update.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.ViewModelKey
import mobi.tarantino.proxykillswitcher.feature.update.data.GitHubReleaseLoader
import mobi.tarantino.proxykillswitcher.feature.update.data.HttpGitHubReleaseLoader
import mobi.tarantino.proxykillswitcher.feature.update.data.HttpUpdateDownloader
import mobi.tarantino.proxykillswitcher.feature.update.data.IntentApkInstaller
import mobi.tarantino.proxykillswitcher.feature.update.data.PackageManagerApkVerifier
import mobi.tarantino.proxykillswitcher.feature.update.data.PackageManagerInstalledAppInfo
import mobi.tarantino.proxykillswitcher.feature.update.data.SystemUpdateClock
import mobi.tarantino.proxykillswitcher.feature.update.domain.ApkInstaller
import mobi.tarantino.proxykillswitcher.feature.update.domain.DownloadedApkVerifier
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstalledAppInfo
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateClock
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateDownloader
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateViewModel

@BindingContainer
@ContributesTo(AppScope::class)
interface UpdateModule {
    @Binds
    @IntoMap
    @ViewModelKey(UpdateViewModel::class)
    fun bindUpdateViewModel(updateViewModel: UpdateViewModel): ViewModel

    @Binds
    fun bindGitHubReleaseLoader(httpGitHubReleaseLoader: HttpGitHubReleaseLoader): GitHubReleaseLoader

    @Binds
    fun bindUpdateDownloader(httpUpdateDownloader: HttpUpdateDownloader): UpdateDownloader

    @Binds
    fun bindDownloadedApkVerifier(packageManagerApkVerifier: PackageManagerApkVerifier): DownloadedApkVerifier

    @Binds
    fun bindApkInstaller(intentApkInstaller: IntentApkInstaller): ApkInstaller

    @Binds
    fun bindInstalledAppInfo(packageManagerInstalledAppInfo: PackageManagerInstalledAppInfo): InstalledAppInfo

    @Binds
    fun bindUpdateClock(systemUpdateClock: SystemUpdateClock): UpdateClock
}
