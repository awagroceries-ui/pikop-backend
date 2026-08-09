package com.ng.pikop.core.network

import com.google.gson.Gson
import retrofit2.HttpException

object ErrorUtils {
    fun parseError(e: Exception): String {
        return if (e is HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                // Use a map to safely extract the message regardless of the response class
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                val errorMap: Map<String, String> = Gson().fromJson(errorBody, mapType)
                errorMap["message"] ?: errorMap["error"] ?: "An error occurred. Please try again."
            } catch (ex: Exception) {
                "Service temporary unavailable (Error ${e.code()})"
            }
        } else {
            e.localizedMessage ?: "Connection failed. Please check your internet."
        }
    }
}
