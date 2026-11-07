package mobi.tarantino.proxykillswitcher.feature.update.domain

sealed interface InstallResult {
    data object InstallerLaunched : InstallResult

    data object UnknownSourcesRequired : InstallResult

    data object Failed : InstallResult
}
