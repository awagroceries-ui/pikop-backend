package com.ng.pikop.core.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {
    private var socket: Socket? = null
    private var currentUserId: String? = null
    private const val SOCKET_URL = "https://api.pikop.com.ng"
    private var authToken: String? = null

    /**
     * Gives the socket the user's login token. The server uses it to know who
     * is connected. An already-open connection stays open when the token is
     * refreshed; the next new connection uses the fresh token.
     */
    fun setToken(token: String?) {
        authToken = token
    }

    fun connect(userId: String? = null) {
        val safeUserId = userId ?: currentUserId
        if (authToken.isNullOrBlank()) {
            android.util.Log.w("PikopSocket", "Connection aborted: no login token yet")
            return
        }
        if (safeUserId == null || safeUserId == "null" || safeUserId == "0") {
            android.util.Log.w("PikopSocket", "Connection aborted: Invalid userId")
            return
        }

        if (socket != null && currentUserId != safeUserId) {
            socket?.disconnect()
            socket = null
        }

        if (socket == null) {
            currentUserId = safeUserId
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                
                // MILSTONE: Multi-Handshake Pattern for VPS Reliability
                auth = mapOf("token" to authToken!!, "userId" to safeUserId)
                query = "userId=$safeUserId"
            }
            socket = IO.socket(SOCKET_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                android.util.Log.d("PikopSocket", "✅ Socket CONNECTED: ${socket?.id()}")
            }
            socket?.on(Socket.EVENT_DISCONNECT) {
                android.util.Log.w("PikopSocket", "❌ Socket DISCONNECTED")
            }
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                android.util.Log.e("PikopSocket", "⛔ Connection Error: ${args.getOrNull(0)}")
            }
        }
        
        if (socket?.connected() == false) {
            socket?.connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        authToken = null
    }

    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
    }

    fun on(event: String, callback: (JSONObject) -> Unit) {
        socket?.on(event) { args ->
            android.util.Log.d("PikopSocket", "Event received: $event")
            if (args.isEmpty()) {
                // Trigger callback even without data (for connect/disconnect)
                callback(JSONObject())
            } else {
                val data = args[0]
                if (data is JSONObject) {
                    callback(data)
                } else if (data is String) {
                    try {
                        callback(JSONObject(data))
                    } catch (e: Exception) {
                        callback(JSONObject().put("message", data))
                    }
                }
            }
        }
    }

    fun off(event: String) {
        socket?.off(event)
    }
}
