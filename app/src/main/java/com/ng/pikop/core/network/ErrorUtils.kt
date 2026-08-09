package com.ng.pikop.core.network

import com.google.gson.Gson
import retrofit2.HttpException

object ErrorUtils {
    fun parseError(e: Exception): String {
        return if (e is HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                val response = Gson().fromJson(errorBody, AuthResponse::class.java)
                response.message ?: "An error occurred. Please try again."
            } catch (ex: Exception) {
                "Server error: ${e.code()}"
            }
        } else {
            e.localizedMessage ?: "Connection failed. Please check your internet."
        }
    }
}
