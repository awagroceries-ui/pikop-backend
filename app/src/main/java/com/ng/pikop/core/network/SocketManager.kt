package com.ng.pikop.core.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {
    private var socket: Socket? = null
    private var currentUserId: String? = null
    private const val SOCKET_URL = "https://api.awa.name.ng/"

    fun connect(userId: String? = null) {
        if (socket != null && currentUserId != userId) {
            socket?.disconnect()
            socket = null
        }

        if (socket == null) {
            currentUserId = userId
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                if (userId != null) {
                    query = "userId=$userId"
                }
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
    }

    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
    }

    fun on(event: String, callback: (JSONObject) -> Unit) {
        socket?.on(event) { args ->
            android.util.Log.d("PikopSocket", "Event received: $event")
            if (args.isNotEmpty()) {
                val data = args[0]
                if (data is JSONObject) {
                    callback(data)
                } else if (data is String) {
                    try {
                        callback(JSONObject(data))
                    } catch (e: Exception) {}
                }
            }
        }
    }

    fun off(event: String) {
        socket?.off(event)
    }
}
