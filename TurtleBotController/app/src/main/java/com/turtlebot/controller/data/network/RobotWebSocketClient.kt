package com.turtlebot.controller.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.turtlebot.controller.data.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

class RobotWebSocketClient(
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionLost: () -> Unit,
    private val context: Context? = null
) {
    private val tag = "RobotWebSocket"

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(3, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private fun getWifiSocketFactory(): SocketFactory? {
        val ctx = context ?: return null
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val wifiNetwork = cm.allNetworks.firstOrNull { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
                wifiNetwork?.socketFactory
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Failed to query Wi-Fi socket factory: ${e.localizedMessage}")
            null
        }
    }

    private fun isPrivateSubnet(host: String): Boolean {
        return host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.") ||
                host == "127.0.0.1" ||
                host == "localhost"
    }

    @Synchronized
    fun connect(host: String, port: Int) {
        val current = _connectionState.value
        if (current is ConnectionState.Connected || current is ConnectionState.Connecting) {
            Log.d(tag, "Already connecting or connected.")
            return
        }

        // Bulletproof Host & Port Sanitization to guarantee compatibility across all physical devices
        var cleanHost = host.trim()
            .removePrefix("ws://")
            .removePrefix("wss://")
            .removePrefix("http://")
            .removePrefix("https://")
            .trim()

        var targetPort = port
        if (cleanHost.contains(":")) {
            val parts = cleanHost.split(":")
            cleanHost = parts[0].trim()
            val parsedPort = parts.getOrNull(1)?.removeSuffix("/")?.trim()?.toIntOrNull()
            if (parsedPort != null && parsedPort in 1..65535) {
                targetPort = parsedPort
            }
        }
        cleanHost = cleanHost.removeSuffix("/").trim()

        if (cleanHost.isEmpty()) {
            _connectionState.value = ConnectionState.Error("Invalid robot IP address")
            onConnectionLost()
            return
        }

        _connectionState.value = ConnectionState.Connecting(cleanHost, targetPort)
        val url = "ws://$cleanHost:$targetPort"
        val request = Request.Builder().url(url).build()

        val clientBuilder = baseClient.newBuilder()
        if (isPrivateSubnet(cleanHost)) {
            val wifiSocketFactory = getWifiSocketFactory()
            if (wifiSocketFactory != null) {
                Log.i(tag, "Binding WebSocket to physical Wi-Fi network interface for private IP $cleanHost")
                clientBuilder.socketFactory(wifiSocketFactory)
            }
        }
        val activeClient = clientBuilder.build()

        webSocket = activeClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(tag, "WebSocket connection opened to $url")
                _connectionState.value = ConnectionState.Connected(cleanHost, targetPort)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                onMessageReceived(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w(tag, "WebSocket closing ($code): $reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w(tag, "WebSocket closed ($code): $reason")
                _connectionState.value = ConnectionState.Disconnected
                onConnectionLost()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val userFriendlyMessage = when (t) {
                    is SocketTimeoutException -> "Connection timed out: $cleanHost:$targetPort unreachable. Verify phone is connected to robot Wi-Fi."
                    is ConnectException -> "Connection refused: verify python3 server.py is running on Raspberry Pi port $targetPort."
                    is UnknownHostException -> "Cannot resolve host '$cleanHost'. Check IP address format."
                    else -> t.localizedMessage ?: "Connection failed"
                }
                Log.e(tag, "WebSocket connection failed: $userFriendlyMessage", t)
                _connectionState.value = ConnectionState.Error(userFriendlyMessage)
                onConnectionLost()
            }
        })
    }

    @Synchronized
    fun disconnect() {
        Log.i(tag, "Disconnecting WebSocket client explicitly...")
        try {
            webSocket?.close(1000, "Client initiated disconnect")
        } catch (e: Exception) {
            Log.e(tag, "Error closing websocket", e)
        } finally {
            webSocket = null
            _connectionState.value = ConnectionState.Disconnected
            onConnectionLost()
        }
    }

    @Synchronized
    fun send(message: String): Boolean {
        val ws = webSocket ?: return false
        return ws.send(message)
    }

    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    suspend fun testReachability(host: String, port: Int, timeoutMs: Int = 3000): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            var cleanHost = host.trim()
                .removePrefix("ws://")
                .removePrefix("wss://")
                .removePrefix("http://")
                .removePrefix("https://")
                .trim()
            if (cleanHost.contains(":")) {
                cleanHost = cleanHost.substringBefore(":")
            }
            cleanHost = cleanHost.removeSuffix("/").trim()

            if (cleanHost.isEmpty()) {
                return@withContext Pair(false, "IP address is empty")
            }

            val startTime = System.currentTimeMillis()
            val socket = Socket()
            try {
                val wifiSocketFactory = if (isPrivateSubnet(cleanHost)) getWifiSocketFactory() else null
                val socketToUse = wifiSocketFactory?.createSocket() ?: socket
                val socketAddress = InetSocketAddress(cleanHost, port)
                socketToUse.connect(socketAddress, timeoutMs)
                val latency = System.currentTimeMillis() - startTime
                socketToUse.close()
                Pair(true, "Reachable! Latency: ${latency}ms")
            } catch (e: SocketTimeoutException) {
                Pair(false, "Timeout after ${timeoutMs}ms ($cleanHost:$port unreachable)")
            } catch (e: ConnectException) {
                Pair(false, "Connection refused ($cleanHost reachable, but server.py not running on port $port)")
            } catch (e: Exception) {
                Pair(false, e.localizedMessage ?: "Failed to reach host")
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
}
