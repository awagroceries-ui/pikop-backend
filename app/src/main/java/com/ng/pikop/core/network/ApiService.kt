package com.ng.pikop.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class SignupRequest(
    val full_name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String
)

data class AuthResponse(
    val message: String,
    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
    val email: String?,
    val role: String?
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
    val item_description: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val delivery_lat: Double,
    val delivery_lng: Double
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
    val notes: String?,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val delivery_lat: Double,
    val delivery_lng: Double,
    val item_photo_url: String,
    val pickup_display_summary: String,
    val delivery_display_summary: String
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
    val pickup_lat: Double,
    val pickup_lng: Double,
    val delivery_lat: Double,
    val delivery_lng: Double,
    val item_photo_url: String?,
    val history: List<StatusHistoryItem>
)

data class FulfillerProfileResponse(
    val id: Int,
    val online_status: String,
    val kyc_status: String
)

data class FulfillerStatusRequest(
    val online_status: String,
    val lat: Double? = null,
    val lng: Double? = null
)

data class FulfillerOrderResponse(
    val id: Int,
    val status: String,
    val total_fare: Double,
    val earnings: Double,
    val pickup_address: String,
    val delivery_address: String,
    val created_at: String
)

data class OfferResponse(
    val id: String,
    val pickup_address: String,
    val delivery_address: String,
    val total_fare: Double,
    val item_photo_url: String?,
    val expires_at: String
)

data class VerifyCodeRequest(
    val code: String,
    val delivery_photo_url: String? = null
)

data class RatingRequest(
    val rating: Int,
    val comment: String? = null
)

data class IncidentRequest(
    val category: String, // breakdown, accident, security_risk, other
    val description: String,
    val resolution_requested: String // handoff, cancel_with_waiver_request
)

data class SavedAddress(
    val id: Int,
    val label: String,
    val address_text: String,
    val lat: Double,
    val lng: Double
)

data class AddressRequest(
    val label: String,
    val address_text: String,
    val lat: Double,
    val lng: Double
)

data class WalletTransaction(
    val id: Int,
    val amount: Double,
    val entry_type: String, // CREDIT, DEBIT
    val purpose: String,
    val created_at: String
)

data class WalletResponse(
    val balance: Double,
    val currency: String,
    val transactions: List<WalletTransaction>
)

data class WithdrawalRequest(
    val amount: Double,
    val type: String // STANDARD, INSTANT
)

interface ApiService {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("api/v1/auth/fcm-token")
    suspend fun updateFCMToken(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/orders/quote")
    suspend fun getQuote(@Body request: QuoteRequest): QuoteResponse

    @POST("api/v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): OrderResponse

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrderDetails(@retrofit2.http.Path("orderId") orderId: String): OrderDetailsResponse

    @GET("api/v1/orders")
    suspend fun getUserOrders(): List<OrderDetailsResponse>

    @PATCH("api/v1/fulfillers/status")
    suspend fun updateStatus(@Body request: FulfillerStatusRequest): AuthResponse

    @GET("api/v1/fulfillers/profile")
    suspend fun getFulfillerProfile(): FulfillerProfileResponse

    @GET("api/v1/fulfillers/offers")
    suspend fun getOffers(): List<OfferResponse>

    @GET("api/v1/fulfillers/orders")
    suspend fun getFulfillerOrders(): List<FulfillerOrderResponse>

    @Multipart
    @POST("api/v1/fulfillers/kyc")
    suspend fun uploadKYC(
        @Part("document_type") type: RequestBody,
        @Part document: MultipartBody.Part
    ): AuthResponse

    @Multipart
    @POST("api/v1/orders/upload")
    suspend fun uploadOrderPhoto(
        @Part document: MultipartBody.Part
    ): Map<String, String>

    @POST("api/v1/orders/{id}/accept")
    suspend fun acceptOrder(@retrofit2.http.Path("id") id: String): OrderResponse

    @POST("api/v1/orders/{id}/pickup")
    suspend fun verifyPickup(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @POST("api/v1/orders/{id}/deliver")
    suspend fun verifyDelivery(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @POST("api/v1/orders/{id}/cancel")
    suspend fun cancelOrder(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): AuthResponse

    @POST("api/v1/orders/{id}/incident")
    suspend fun fileIncident(@retrofit2.http.Path("id") id: String, @Body request: IncidentRequest): AuthResponse

    @GET("api/v1/orders/me/queue-candidates")
    suspend fun getQueueCandidates(): List<OfferResponse>

    @POST("api/v1/orders/{id}/queue/claim")
    suspend fun claimQueueOrder(@retrofit2.http.Path("id") id: String): AuthResponse

    @POST("api/v1/orders/{id}/rate")
    suspend fun rateCustomer(@retrofit2.http.Path("id") id: String, @Body request: RatingRequest): OrderResponse

    @GET("api/v1/addresses")
    suspend fun getSavedAddresses(): List<SavedAddress>

    @POST("api/v1/addresses")
    suspend fun saveAddress(@Body request: AddressRequest): SavedAddress

    @DELETE("api/v1/addresses/{id}")
    suspend fun deleteAddress(@retrofit2.http.Path("id") id: Int): AuthResponse

    @GET("api/v1/wallets/me")
    suspend fun getWalletInfo(): WalletResponse

    @POST("api/v1/withdrawals")
    suspend fun requestWithdrawal(@Body request: WithdrawalRequest): AuthResponse

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
