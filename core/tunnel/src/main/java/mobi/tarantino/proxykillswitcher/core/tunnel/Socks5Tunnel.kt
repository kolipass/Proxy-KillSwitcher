package mobi.tarantino.proxykillswitcher.core.tunnel

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import java.io.File

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class Socks5Tunnel : TunnelEngine {
    private var httpConnectSocksBridge: HttpConnectSocksBridge? = null
    private var protectFileDescriptor: (Int) -> Boolean = { false }

    override fun start(
        tunFileDescriptor: Int,
        config: TunnelEngine.Config,
    ): Boolean {
        stop()
        protectFileDescriptor = config.protectFileDescriptor
        val configFile = File(config.filesDir, CONFIG_FILE_NAME)
        val startedHttpConnectSocksBridge =
            HttpConnectSocksBridge(
                protectSocket = config.protectSocket,
                onUpstreamUnreachable = config.onUpstreamUnreachable,
            )
        httpConnectSocksBridge = startedHttpConnectSocksBridge
        val socksPort =
            startedHttpConnectSocksBridge.start(
                upstreamHost = config.upstreamHost,
                upstreamPort = config.upstreamPort,
            )
        configFile.writeText(
            createConfig(host = LOOPBACK_HOST, port = socksPort),
        )
        activeTunnel = this
        return TProxyStartService(configFile.absolutePath, tunFileDescriptor)
    }

    override fun stop() {
        TProxyStopService()
        httpConnectSocksBridge?.stop()
        httpConnectSocksBridge = null
        activeTunnel = null
    }

    private fun createConfig(
        host: String,
        port: Int,
    ): String {
        val escapedHost = host.replace("'", "''")
        return """
            tunnel:
              mtu: 1500
              ipv4: $TUN_ADDRESS
              icmp: 'reply'
            socks5:
              address: '$escapedHost'
              port: $port
              udp: 'udp'
            mapdns:
              address: $MAP_DNS_ADDRESS
              port: $MAP_DNS_PORT
              network: $MAP_DNS_NETWORK
              netmask: $MAP_DNS_NETMASK
              cache-size: $MAP_DNS_CACHE_SIZE
            misc:
              log-level: 'warn'
            """.trimIndent()
    }

    companion object {
        private const val CONFIG_FILE_NAME = "kill-switch-socks5.yaml"
        private const val TUN_ADDRESS = "10.10.0.2"
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val MAP_DNS_ADDRESS = "10.10.0.1"
        private const val MAP_DNS_PORT = 53
        private const val MAP_DNS_NETWORK = "10.11.0.0"
        private const val MAP_DNS_NETMASK = "255.255.0.0"
        private const val MAP_DNS_CACHE_SIZE = 10000

        @Volatile
        private var activeTunnel: Socks5Tunnel? = null

        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStartService(
            configPath: String,
            tunFileDescriptor: Int,
        ): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStopService(): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyIsRunning(): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyGetStats(): LongArray

        @JvmStatic
        private fun protectSocket(socketFileDescriptor: Int): Boolean {
            return activeTunnel?.protectFileDescriptor?.invoke(socketFileDescriptor) == true
        }
    }
}
