package mobi.tarantino.proxykillswitcher.core.tunnel

import java.io.File
import java.net.Socket

interface TunnelEngine {
    fun start(
        tunFileDescriptor: Int,
        config: Config,
    ): Boolean

    fun stop()

    data class Config(
        val filesDir: File,
        val upstreamHost: String,
        val upstreamPort: Int,
        val protectSocket: (Socket) -> Boolean,
        val protectFileDescriptor: (Int) -> Boolean,
        val onUpstreamUnreachable: () -> Unit,
    )
}
