package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

enum class ProxyProtocol(
    val storedValue: String,
) {
    Http("http"),
    ;

    companion object {
        fun fromStored(storedValue: String): ProxyProtocol {
            return entries.firstOrNull { protocol -> protocol.storedValue == storedValue } ?: Http
        }
    }
}
