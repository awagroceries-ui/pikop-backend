package com.ng.pikop.core.network

import com.ng.pikop.core.datastore.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

data class SignupRequest(
    val full_name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String,
    val referral_code: String? = null
)

data class AuthResponse(
    val message: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val referral_code: String? = null
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
    val total_fare: Double? = null,
    val size_tier: String? = null,
    val fare_locked_until: String? = null
)

data class QuoteResponse(
    val quote_id: String? = null,
    val fare_breakdown: FareBreakdown? = null
)

data class CreateOrderRequest(
    val quote_id: String,
    val corporate_account_id: String? = null,
    val promo_id: String? = null,
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
    val order_id: String? = null,
    val status: String? = null,
    val tracking_url: String? = null,
    val message: String? = null
)

data class StatusHistoryItem(
    val status: String? = null,
    val description: String? = null,
    val time: String? = null
)

data class FulfillerPublicProfile(
    val full_name: String? = null,
    val profile_photo_url: String? = null,
    val tier: String? = null,
    val vehicle_registration_number: String? = null,
    val rating_avg: Double? = null
)

data class OrderDetailsResponse(
    val id: String? = null,
    val status: String? = null,
    val total_fare: Double? = null,
    val pickup_address: String? = null,
    val delivery_address: String? = null,
    val pickup_lat: Double? = null,
    val pickup_lng: Double? = null,
    val delivery_lat: Double? = null,
    val delivery_lng: Double? = null,
    val item_photo_url: String? = null,
    val tracking_url: String? = null,
    val fulfiller_profile: FulfillerPublicProfile? = null,
    val history: List<StatusHistoryItem>? = null
)

data class FulfillerProfileResponse(
    val id: Int? = null,
    val online_status: String? = null,
    val kyc_status: String? = null,
    val didit_verification_status: String? = null,
    val mobility_type: String? = null,
    val profile_photo_url: String? = null,
    val tier: String? = null,
    val primary_class: String? = null,
    val registration_number: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
    val rating_avg: Double? = null
)

data class DiditSessionResponse(
    @SerializedName("url") val url: String? = null,
    @SerializedName("session_token", alternate = ["token"]) val session_token: String? = null,
    @SerializedName("session_id") val session_id: String? = null
)

data class VehicleDetails(
    val registration_number: String,
    val make: String?,
    val model: String?,
    val color: String?
)

data class ProfileUpdateRequest(
    val full_name: String? = null,
    val phone: String? = null,
    val mobility_type: String? = null,
    val primary_class: String? = null,
    val vehicle_details: VehicleDetails? = null
)

data class UserProfileResponse(
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null
)

data class FulfillerStatusRequest(
    val online_status: String,
    val lat: Double? = null,
    val lng: Double? = null
)

data class FulfillerOrderResponse(
    val id: Int? = null,
    val status: String? = null,
    val total_fare: Double? = null,
    val earnings: Double? = null,
    val pickup_address: String? = null,
    val delivery_address: String? = null,
    val created_at: String? = null
)

data class OfferResponse(
    val id: String? = null,
    val pickup_address: String? = null,
    val delivery_address: String? = null,
    val total_fare: Double? = null,
    val item_photo_url: String? = null,
    val expires_at: String? = null
)

data class VerifyCodeRequest(
    val code: String,
    val delivery_photo_url: String? = null,
    val signature_photo_url: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val device_timestamp: Long? = null
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
    val id: Int? = null,
    val label: String? = null,
    val address_text: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

data class SavedRecipient(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val label: String? = null
)

data class RecipientRequest(
    val name: String,
    val phone: String,
    val label: String? = null
)

data class UserSession(
    val id: String? = null,
    val device_name: String? = null,
    val ip_address: String? = null,
    val last_active: String? = null,
    val created_at: String? = null
)

data class AddressRequest(
    val label: String,
    val address_text: String,
    val lat: Double,
    val lng: Double
)

data class WalletTransaction(
    val id: Int? = null,
    val amount: Double? = null,
    val entry_type: String? = null, // CREDIT, DEBIT
    val purpose: String? = null,
    val created_at: String? = null
)

data class WalletResponse(
    val balance: Double? = null,
    val currency: String? = null,
    val transactions: List<WalletTransaction>? = null
)

data class SupportConversation(
    val id: String? = null,
    val status: String? = null
)

data class ChatMessage(
    val id: String? = null,
    val sender_id: Int? = null,
    val sender_type: String? = null,
    val body: String? = null,
    val content: String? = null, // Support backward compatibility
    val created_at: String? = null
)

data class WithdrawalRequest(
    val amount: Double,
    val type: String // STANDARD, INSTANT
)

data class CorporateAccount(
    val id: String? = null,
    val company_name: String? = null,
    val billing_email: String? = null,
    val billing_type: String? = null, // direct_debit, prepaid_wallet
    val status: String? = null
)

data class CreateCorporateRequest(
    val company_name: String,
    val billing_email: String,
    val billing_type: String
)

data class MandateResponse(
    val authorization_url: String? = null,
    val message: String? = null
)

data class CorporateStaff(
    val full_name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val created_at: String? = null
)

data class PromoValidationResponse(
    val promo_id: String? = null,
    val discount_type: String? = null, // flat, percentage
    val value: Double? = null,
    val message: String? = null
)

data class PaymentInitializationRequest(
    val amount: Long, // in Kobo
    val email: String,
    val metadata: Map<String, String>? = null
)

data class PaymentInitializationResponse(
    val authorization_url: String,
    val access_code: String,
    val reference: String
)

interface ApiService {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("api/v1/auth/resend-otp")
    suspend fun resendOtp(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/fcm-token")
    suspend fun updateFCMToken(@Body request: Map<String, String>): AuthResponse

    @PATCH("api/v1/settings/notifications")
    suspend fun updateNotificationPrefs(@Body request: Map<String, Boolean>): AuthResponse

    @POST("api/v1/promo-codes/validate")
    suspend fun validatePromoCode(@Body request: Map<String, String>): PromoValidationResponse

    @POST("api/v1/orders/quote")
    suspend fun getQuote(@Body request: QuoteRequest): QuoteResponse

    @POST("api/v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): OrderResponse

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrderDetails(@retrofit2.http.Path("orderId") orderId: String): OrderDetailsResponse

    @GET("api/v1/orders")
    suspend fun getUserOrders(): List<OrderDetailsResponse>

    @PATCH("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): OrderResponse

    @PATCH("api/v1/fulfillers/status")
    suspend fun updateStatus(@Body request: FulfillerStatusRequest): AuthResponse

    @GET("api/v1/fulfillers/profile")
    suspend fun getFulfillerProfile(): FulfillerProfileResponse

    @PATCH("api/v1/fulfillers/profile")
    suspend fun updateFulfillerProfile(@Body request: ProfileUpdateRequest): AuthResponse

    @Multipart
    @POST("api/v1/fulfillers/profile-photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): AuthResponse

    @POST("api/v1/fulfillers/submit-application")
    suspend fun submitApplication(): AuthResponse

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
    suspend fun acceptOrder(@retrofit2.http.Path("id") id: String, @Body request: Map<String, Int>): OrderResponse

    @POST("api/v1/orders/{id}/pickup")
    suspend fun verifyPickup(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @POST("api/v1/orders/{id}/deliver")
    suspend fun verifyDelivery(@retrofit2.http.Path("id") id: String, @Body request: VerifyCodeRequest): OrderResponse

    @GET("api/v1/orders/{orderId}/messages")
    suspend fun getOrderMessages(@retrofit2.http.Path("orderId") orderId: String): List<ChatMessage>

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

    @GET("api/v1/settings/recipients")
    suspend fun getSavedRecipients(): List<SavedRecipient>

    @POST("api/v1/settings/recipients")
    suspend fun addRecipient(@Body request: RecipientRequest): SavedRecipient

    @DELETE("api/v1/settings/recipients/{id}")
    suspend fun deleteRecipient(@retrofit2.http.Path("id") id: String): AuthResponse

    @GET("api/v1/settings/sessions")
    suspend fun getActiveSessions(): List<UserSession>

    @DELETE("api/v1/settings/sessions/{id}")
    suspend fun revokeSession(@retrofit2.http.Path("id") id: String): AuthResponse

    @POST("api/v1/payments/initialize")
    suspend fun initializePayment(@retrofit2.http.Body request: PaymentInitializationRequest): PaymentInitializationResponse

    @POST("api/v1/addresses")
    suspend fun saveAddress(@Body request: AddressRequest): SavedAddress

    @DELETE("api/v1/addresses/{id}")
    suspend fun deleteAddress(@retrofit2.http.Path("id") id: Int): AuthResponse

    @POST("api/v1/support/conversations")
    suspend fun getOrCreateSupportConversation(): SupportConversation

    @GET("api/v1/support/conversations/{id}/messages")
    suspend fun getSupportMessages(@retrofit2.http.Path("id") id: String): List<ChatMessage>

    @GET("api/v1/wallets/me")
    suspend fun getWalletInfo(): WalletResponse

    @POST("api/v1/withdrawals")
    suspend fun requestWithdrawal(@Body request: WithdrawalRequest): AuthResponse

    @POST("api/v1/fulfillers/kyc/start-verification")
    suspend fun startDiditVerification(): DiditSessionResponse

    @POST("api/v1/corporate/accounts")
    suspend fun createCorporateAccount(@Body request: CreateCorporateRequest): CorporateAccount

    @GET("api/v1/corporate/my-accounts")
    suspend fun getMyCorporateAccounts(): List<CorporateAccount>

    @POST("api/v1/corporate/accounts/{id}/mandate/authorize")
    suspend fun authorizeMandate(@retrofit2.http.Path("id") id: String): MandateResponse

    @POST("api/v1/corporate/accounts/{id}/sub-accounts")
    suspend fun addStaffToCorporate(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): AuthResponse

    @GET("api/v1/corporate/accounts/{id}/sub-accounts")
    suspend fun getCorporateStaff(@retrofit2.http.Path("id") id: String): List<CorporateStaff>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: Map<String, String>): AuthResponse

    @GET("api/v1/settings/profile")
    suspend fun getUserProfile(): UserProfileResponse

    @PATCH("api/v1/settings/profile")
    suspend fun updateUserProfile(@Body request: ProfileUpdateRequest): AuthResponse

    companion object {
        private const val BASE_URL = "https://api.awa.name.ng/"

        fun create(tokenManager: TokenManager): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val token = runBlocking {
                    tokenManager.accessToken.first()
                }
                val request = chain.request().newBuilder().apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }

            val authenticator = okhttp3.Authenticator { _, response ->
                android.util.Log.d("PikopApi", "Authenticator triggered: ${response.code}")
                if (response.code == 401) {
                    synchronized(this) {
                        val newToken = runBlocking {
                            try {
                                val refreshToken = tokenManager.refreshToken.first()
                                android.util.Log.d("PikopApi", "Attempting refresh with token: ${refreshToken?.take(10)}...")
                                if (refreshToken != null) {
                                    val api = Retrofit.Builder()
                                        .baseUrl(BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                        .create(ApiService::class.java)

                                    val res = api.refresh(mapOf("refreshToken" to refreshToken))
                                    if (res.accessToken != null && res.refreshToken != null) {
                                        android.util.Log.d("PikopApi", "Token refresh SUCCESS")
                                        tokenManager.saveTokens(
                                            res.accessToken,
                                            res.refreshToken,
                                            tokenManager.userEmail.first() ?: "",
                                            tokenManager.userRole.first() ?: "",
                                            tokenManager.referralCode.first()
                                        )
                                        res.accessToken
                                    } else {
                                        android.util.Log.e("PikopApi", "Token refresh failed: Missing fields in response")
                                        null
                                    }
                                } else {
                                    android.util.Log.e("PikopApi", "Token refresh failed: No refresh token found in DataStore")
                                    null
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("PikopApi", "Token refresh exception: ${e.message}")
                                null
                            }
                        }

                        if (newToken != null) {
                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                        } else {
                            null
                        }
                    }
                } else null
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
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
