package mobi.tarantino.proxykillswitcher.feature.update.domain

sealed interface UpdateCheck {
    data object NoUpdate : UpdateCheck

    data object Throttled : UpdateCheck

    data object Failed : UpdateCheck

    data class Available(
        val update: AvailableUpdate,
        val installMode: UpdateInstallMode,
    ) : UpdateCheck
}
