package com.ng.pikop.core.network

import com.google.gson.Gson
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorUtils {
    fun parseError(e: Exception): String {
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    502, 503 -> return "Our servers are currently undergoing maintenance. Please try again in a few minutes."
                    504 -> return "The server took too long to respond. Please check your connection."
                }
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody.isNullOrBlank()) return "Server returned error ${e.code()}"
                    
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                    val errorMap: Map<String, String>? = Gson().fromJson(errorBody, mapType)
                    
                    errorMap?.get("message") ?: errorMap?.get("error") ?: "Protocol error: ${e.code()}"
                } catch (ex: Exception) {
                    "Service temporary unavailable (Error ${e.code()})"
                }
            }
            is SocketTimeoutException -> "Request timed out. Please try again."
            is UnknownHostException -> "No internet connection detected."
            else -> e.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }
}
