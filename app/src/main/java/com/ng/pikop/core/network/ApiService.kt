package com.ng.pikop.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class SignupRequest(
    val full_name: String,
    val email: String,
    val phone: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
    val email: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class VerifyEmailRequest(
    val email: String,
    val otp: String
)

data class QuoteRequest(
    val pickup_address: String,
    val delivery_address: String,
    val item_description: String
)

data class FareBreakdown(
    val total_fare: Double,
    val size_tier: String,
    val fare_locked_until: String
)

data class QuoteResponse(
    val quote_id: String,
    val fare_breakdown: FareBreakdown
)

data class CreateOrderRequest(
    val quote_id: String,
    val payment_method: String,
    val recipient_name: String,
    val recipient_phone: String,
    val notes: String?
)

data class OrderResponse(
    val order_id: String,
    val status: String,
    val message: String?
)

data class StatusHistoryItem(
    val status: String,
    val description: String,
    val time: String
)

data class OrderDetailsResponse(
    val id: String,
    val status: String,
    val total_fare: Double,
    val pickup_address: String,
    val delivery_address: String,
    val history: List<StatusHistoryItem>
)

data class FulfillerStatusRequest(
    val online_status: String
)

data class OfferResponse(
    val id: String,
    val pickup_address: String,
    val delivery_address: String,
    val total_fare: Double,
    val expires_at: String
)

data class VerifyCodeRequest(
    val code: String
)

data class RatingRequest(
    val rating: Int,
    val comment: String? = null
)

interface ApiService {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("api/v1/orders/quote")
    suspend fun getQuote(@Body request: QuoteRequest): QuoteResponse

    @POST("api/v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): OrderResponse

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrderDetails(@retrofit2.http.Path("orderId") orderId: String): OrderDetailsResponse

    @PATCH("api/v1/fulfillers/status")
    suspend fun updateStatus(@Body request: FulfillerStatusRequest): AuthResponse

    @GET("api/v1/fulfillers/offers")
    suspend fun getOffers(): List<OfferResponse>

    @POST("api/v1/orders/{id}/accept")
    suspend fun acceptOrder(@retrofit2.http.Path("id") id: String): OrderResponse

    @POST("api/v1/orders/{id}/pickup")
    suspend fun verifyPickup(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @POST("api/v1/orders/{id}/deliver")
    suspend fun verifyDelivery(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @POST("api/v1/orders/{id}/rate")
    suspend fun rateCustomer(@retrofit2.http.Path("id") id: String, @Body request: RatingRequest): OrderResponse

    companion object {
        private const val BASE_URL = "https://api.awa.name.ng/"

        fun create(): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
