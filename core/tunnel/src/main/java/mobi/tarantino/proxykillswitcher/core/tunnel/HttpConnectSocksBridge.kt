package mobi.tarantino.proxykillswitcher.core.tunnel

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HttpConnectSocksBridge(
    private val protectSocket: (Socket) -> Boolean,
    private val onUpstreamUnreachable: () -> Unit = {},
) {
    private val isRunning = AtomicBoolean(false)
    private val acceptExecutor = Executors.newSingleThreadExecutor()
    private val sessionExecutor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null

    fun start(
        upstreamHost: String,
        upstreamPort: Int,
    ): Int {
        stop()
        val boundServerSocket = ServerSocket()
        boundServerSocket.bind(InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), 0))
        serverSocket = boundServerSocket
        isRunning.set(true)
        acceptExecutor.execute {
            acceptConnections(
                boundServerSocket = boundServerSocket,
                upstreamHost = upstreamHost,
                upstreamPort = upstreamPort,
            )
        }
        return boundServerSocket.localPort
    }

    fun stop() {
        isRunning.set(false)
        serverSocket?.close()
        serverSocket = null
    }

    private fun acceptConnections(
        boundServerSocket: ServerSocket,
        upstreamHost: String,
        upstreamPort: Int,
    ) {
        while (isRunning.get()) {
            val clientSocket =
                try {
                    boundServerSocket.accept()
                } catch (_: IOException) {
                    break
                }
            sessionExecutor.execute {
                handleClient(
                    clientSocket = clientSocket,
                    upstreamHost = upstreamHost,
                    upstreamPort = upstreamPort,
                )
            }
        }
    }

    private fun handleClient(
        clientSocket: Socket,
        upstreamHost: String,
        upstreamPort: Int,
    ) {
        try {
            relayHttpConnect(
                clientSocket = clientSocket,
                upstreamHost = upstreamHost,
                upstreamPort = upstreamPort,
            )
        } catch (_: IOException) {
            return
        }
    }

    private fun relayHttpConnect(
        clientSocket: Socket,
        upstreamHost: String,
        upstreamPort: Int,
    ) {
        clientSocket.use { client ->
            client.soTimeout = HANDSHAKE_TIMEOUT_MS
            val clientInput = client.getInputStream()
            val clientOutput = client.getOutputStream()
            if (!Socks5ConnectProtocol.completeGreeting(clientInput = clientInput, clientOutput = clientOutput)) {
                return
            }
            val destination = Socks5ConnectProtocol.readConnectDestination(clientInput)
            if (destination == null) {
                Socks5ConnectProtocol.writeFailure(clientOutput)
                return
            }
            val upstreamSocket = createProtectedSocket()
            if (upstreamSocket == null) {
                Socks5ConnectProtocol.writeFailure(clientOutput)
                return
            }
            upstreamSocket.use { upstream ->
                if (!connectToUpstream(upstream = upstream, upstreamHost = upstreamHost, upstreamPort = upstreamPort)) {
                    Socks5ConnectProtocol.writeFailure(clientOutput)
                    onUpstreamUnreachable()
                    return
                }
                upstream.soTimeout = HANDSHAKE_TIMEOUT_MS
                if (!HttpConnectProtocol.completeConnect(upstream = upstream, destination = destination)) {
                    Socks5ConnectProtocol.writeFailure(clientOutput)
                    return
                }
                Socks5ConnectProtocol.writeSuccess(clientOutput)
                client.soTimeout = 0
                upstream.soTimeout = 0
                splice(client = client, upstream = upstream)
            }
        }
    }

    private fun connectToUpstream(
        upstream: Socket,
        upstreamHost: String,
        upstreamPort: Int,
    ): Boolean {
        return try {
            upstream.connect(InetSocketAddress(upstreamHost, upstreamPort), HANDSHAKE_TIMEOUT_MS)
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun createProtectedSocket(): Socket? {
        val upstreamSocket = Socket()
        upstreamSocket.bind(EPHEMERAL_LOCAL_ENDPOINT)
        if (protectSocket(upstreamSocket)) {
            return upstreamSocket
        }
        upstreamSocket.close()
        return null
    }

    private fun splice(
        client: Socket,
        upstream: Socket,
    ) {
        val clientToUpstream =
            sessionExecutor.submit {
                copyStream(inputStream = client.getInputStream(), outputStream = upstream.getOutputStream())
                shutdownOutput(upstream)
            }
        copyStream(inputStream = upstream.getInputStream(), outputStream = client.getOutputStream())
        shutdownOutput(client)
        clientToUpstream.get()
    }

    private fun shutdownOutput(socket: Socket) {
        try {
            socket.shutdownOutput()
        } catch (_: IOException) {
            return
        }
    }

    private fun copyStream(
        inputStream: InputStream,
        outputStream: OutputStream,
    ) {
        try {
            inputStream.copyTo(outputStream)
        } catch (_: IOException) {
            return
        }
    }

    companion object {
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val HANDSHAKE_TIMEOUT_MS = 10_000
        private val EPHEMERAL_LOCAL_ENDPOINT = InetSocketAddress(0)
    }
}
