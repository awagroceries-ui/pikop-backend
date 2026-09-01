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
    val success: Boolean,
    val quote_id: String? = null,
    val size_tier: String? = null,
    val distance_km: String? = null,
    val base_fare: Double? = null,
    val distance_fare: Double? = null,
    val total_fare: Double? = null,
    val expires_at: String? = null
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
    @SerializedName("id") val id: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("total_fare") val total_fare: Double? = null,
    @SerializedName("pickup_address") val pickup_address: String? = null,
    @SerializedName("delivery_address") val delivery_address: String? = null,
    @SerializedName("pickup_lat") val pickup_lat: Double? = null,
    @SerializedName("pickup_lng") val pickup_lng: Double? = null,
    @SerializedName("delivery_lat") val delivery_lat: Double? = null,
    @SerializedName("delivery_lng") val delivery_lng: Double? = null,
    @SerializedName("fulfiller_lat") val fulfiller_lat: Double? = null,
    @SerializedName("fulfiller_lng") val fulfiller_lng: Double? = null,
    @SerializedName("item_photo_url") val item_photo_url: String? = null,
    @SerializedName("tracking_url") val tracking_url: String? = null,
    @SerializedName("recipient_name") val recipient_name: String? = null,
    @SerializedName("recipient_phone") val recipient_phone: String? = null,
    @SerializedName("fulfiller_profile") val fulfiller_profile: FulfillerPublicProfile? = null,
    @SerializedName("history") val history: List<StatusHistoryItem>? = null,
    @SerializedName("data") val data: OrderDetailsResponse? = null
)

data class FulfillerProfileResponse(
    val id: Int? = null,
    val online_status: String? = null,
    val kyc_status: String? = null,
    @SerializedName("didit_verification_status") val kyc_verification_status: String? = null,
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

data class KycSessionResponse(
    val success: Boolean = false,
    val data: KycSessionData? = null,
    // Keep old fields for backward compatibility if any parts of the app rely on flat structure
    @SerializedName("url") val url: String? = null,
    @SerializedName("session_token", alternate = ["token"]) val session_token: String? = null,
    @SerializedName("session_id") val session_id: String? = null
)

data class KycSessionData(
    val url: String? = null,
    val session_id: String? = null,
    val session_token: String? = null
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
    val lng: Double? = null,
    val landmark: String? = null,
    val place_id: String? = null
)

data class SavedAddressesResponse(
    val success: Boolean,
    val addresses: List<SavedAddress>
)

data class AutocompletePrediction(
    val place_id: String,
    val description: String,
    val main_text: String,
    val secondary_text: String
)

data class AutocompleteResponse(
    val success: Boolean,
    val predictions: List<AutocompletePrediction>,
    val error: String? = null
)

data class PlaceDetailsResponse(
    val success: Boolean,
    val formatted_address: String,
    val lat: Double,
    val lng: Double,
    val name: String,
    val address_components: Map<String, String>? = null
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

data class KnowledgeBaseArticle(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val priority: Int
)

data class ChatMessage(
    val id: String? = null,
    val sender_id: Int? = null,
    val sender_type: String? = null,
    val body: String? = null,
    val content: String? = null,
    val text: String? = null, 
    val created_at: String? = null,
    val is_read: Boolean = false
) {
    // Helper to get text regardless of backend column name (content vs body vs text)
    val messageText: String get() = content ?: body ?: text ?: ""
}

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
    val amount: Double, // in Naira
    val email: String,
    val quote_id: String? = null,
    val metadata: Map<String, String>? = null
)

data class PaymentInitializationResponse(
    val authorization_url: String? = null,
    val access_code: String? = null,
    val reference: String? = null
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

    @GET("api/v1/orders/by-quote/{quoteId}")
    suspend fun getOrderByQuote(@retrofit2.http.Path("quoteId") quoteId: String): Map<String, Any>

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
    suspend fun getSavedAddresses(): SavedAddressesResponse

    @POST("api/v1/addresses")
    suspend fun saveAddress(@Body request: SavedAddress): Map<String, Any>

    @GET("api/v1/places/autocomplete")
    suspend fun getAutocomplete(
        @Query("query") query: String,
        @Query("sessionToken") token: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): AutocompleteResponse

    @GET("api/v1/places/details")
    suspend fun getPlaceDetails(
        @Query("placeId") id: String,
        @Query("sessionToken") token: String
    ): PlaceDetailsResponse

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

    @GET("api/v1/payments/verify/{reference}")
    suspend fun verifyPayment(@retrofit2.http.Path("reference") reference: String): Map<String, Any>

    @DELETE("api/v1/addresses/{id}")
    suspend fun deleteAddress(@retrofit2.http.Path("id") id: Int): AuthResponse

    @POST("api/v1/support/conversations")
    suspend fun getOrCreateSupportConversation(): SupportConversation

    @GET("api/v1/support/conversations/{id}/messages")
    suspend fun getSupportMessages(@retrofit2.http.Path("id") id: String): List<ChatMessage>

    @GET("api/v1/support/kb")
    suspend fun getKnowledgeBase(): List<KnowledgeBaseArticle>

    @GET("api/v1/wallets/me")
    suspend fun getWalletInfo(): WalletResponse

    @POST("api/v1/withdrawals")
    suspend fun requestWithdrawal(@Body request: WithdrawalRequest): AuthResponse

    @POST("api/v1/fulfillers/kyc/start")
    suspend fun startKycSession(@Body request: Map<String, String> = mapOf("provider" to "prembly")): KycSessionResponse

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
        private const val BASE_URL = "https://api.pikop.com.ng/"

        fun create(tokenManager: TokenManager): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val token = tokenManager.getAccessTokenSync()
                android.util.Log.d("PikopApi", "Interceptor URL: ${chain.request().url} | Has Token: ${!token.isNullOrBlank()}")
                
                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }

            val authenticator = okhttp3.Authenticator { _, response ->
                android.util.Log.d("PikopApi", "Authenticator -> Code: ${response.code} for: ${response.request.url}")
                
                if (response.code == 401) {
                    synchronized(this) {
                        val refreshToken = tokenManager.getRefreshTokenSync()
                        if (refreshToken.isNullOrBlank()) return@synchronized null

                        val api = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(ApiService::class.java)

                        val res = runBlocking {
                            try {
                                api.refresh(mapOf("refreshToken" to refreshToken))
                            } catch (e: Exception) {
                                android.util.Log.e("PikopApi", "Refresh Call Error: ${e.message}")
                                null
                            }
                        }

                        if (res?.accessToken != null) {
                            android.util.Log.d("PikopApi", "Refresh SUCCESS. Retrying original request.")
                            runBlocking {
                                tokenManager.saveTokens(
                                    accessToken = res.accessToken,
                                    refreshToken = res.refreshToken ?: refreshToken,
                                    email = tokenManager.userEmail.first() ?: "",
                                    role = tokenManager.userRole.first() ?: "CUSTOMER",
                                    name = tokenManager.userName.first(),
                                    phone = tokenManager.userPhone.first(),
                                    isVerified = tokenManager.isVerified.first(),
                                    referralCode = tokenManager.referralCode.first()
                                )
                            }
                            return@Authenticator response.request.newBuilder()
                                .header("Authorization", "Bearer ${res.accessToken}")
                                .build()
                        } else {
                            // Refresh FAILED
                            runBlocking { tokenManager.emitSessionExpired() }
                        }
                    }
                }
                null
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
