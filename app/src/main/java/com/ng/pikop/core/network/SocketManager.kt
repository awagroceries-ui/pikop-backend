package com.ng.pikop.core.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {
    private var socket: Socket? = null
    private const val SOCKET_URL = "https://api.awa.name.ng/"

    fun connect() {
        if (socket == null) {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket(SOCKET_URL, options)
        }
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
    }

    fun on(event: String, callback: (JSONObject) -> Unit) {
        socket?.on(event) { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                callback(args[0] as JSONObject)
            }
        }
    }

    fun off(event: String) {
        socket?.off(event)
    }
}
