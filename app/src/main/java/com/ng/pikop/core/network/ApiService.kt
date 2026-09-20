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
    val referral_code: String? = null,
    
    // Fulfiller specifics
    val primary_class: String? = null, // e.g. RIDER, DRIVER, FOOT_AGENT
    val date_of_birth: String? = null,
    val home_address: String? = null,
    val gender: String? = null,
    val registration_number: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,

    // Legal Consent
    val terms_version: String? = "0.1",
    val privacy_version: String? = "0.1",
    val fleet_invite_code: String? = null
)

data class AuthResponse(
    val success: Boolean? = true,
    val message: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val referral_code: String? = null,
    val status: String? = null,
    @SerializedName("data") val data: AuthResponse? = null
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
    val pickup_landmark: String? = null,
    val delivery_landmark: String? = null,
    val item_description: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val delivery_lat: Double,
    val delivery_lng: Double,
    val item_price: Double? = 0.0,
    val initiator_role: String? = "PAYER",
    val recipient_phone: String? = null,
    val pickup_state: String? = null
)

data class PayerInfo(
    val type: String, // GUEST, APP_USER
    val user_id: Int? = null
)

data class QuoteResponse(
    val success: Boolean,
    val quote_id: String? = null,
    val size_tier: String? = null,
    val distance_km: String? = null,
    val item_price: Double? = null,
    val delivery_fee: Double? = null,
    val platform_fee_amount: Double? = null,
    val fee_payer: String? = null,
    val total_fare: Double? = null,
    val sms_charge_amount: Double? = null,
    val weather_multiplier: Double? = null,
    val traffic_multiplier: Double? = null,
    val surge_multiplier: Double? = 1.0,
    val insurance_fee: Double? = 0.0,
    val recipient_payable: Double? = null,
    val payer_info: PayerInfo? = null,
    val restricted_dispatch: Boolean = false,
    val drivers_count: Int = 0,
    val is_live: Boolean = true,
    val requires_permit: Boolean = false,
    val expires_at: String? = null
)

data class SOSRequest(
    val orderId: String? = null,
    val lat: Double,
    val lng: Double
)

data class JoinWaitlistRequest(
    val city_name: String,
    val state_name: String? = null,
    val email: String
)

data class LandmarkSuggestion(
    val display_text: String,
    val submission_count: Int
)

data class LandmarkSuggestionResponse(
    val success: Boolean,
    val data: List<LandmarkSuggestion>
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
    val delivery_display_summary: String,
    val payment_reference: String? = null,
    val item_price: Double? = null,
    val delivery_fee: Double? = null,
    val seller_phone: String? = null,
    val pickup_state: String? = null,
    val recipient_payable: Double? = null,
    val scheduled_at: String? = null,
    val insurance_fee: Double? = null,
    val is_insured: Boolean = false,
    val surge_multiplier: Double? = null
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
    val make: String? = null,
    val mobility_type: String? = null,
    val primary_class: String? = null,
    val rating_avg: Double? = null,
    val rating_count: Int? = null,
    val kyc_status: String? = null
)

data class OrderDetailsResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("item_description") val item_description: String? = null,
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
    @SerializedName("item_price") val item_price: Double? = null,
    @SerializedName("delivery_fee") val delivery_fee: Double? = null,
    @SerializedName("platform_fee_amount") val platform_fee_amount: Double? = null,
    @SerializedName("sms_charge_amount") val sms_charge_amount: Double? = null,
    @SerializedName("fee_payer") val fee_payer: String? = null,
    @SerializedName("escrow_status") val escrow_status: String? = null,
    @SerializedName("seller_id") val seller_id: Int? = null,
    @SerializedName("merchant_commission_amount") val merchant_commission_amount: Double? = null,
    @SerializedName("grace_period_expires_at") val grace_period_expires_at: String? = null,
    @SerializedName("customer_rating") val customer_rating: Int? = null,
    @SerializedName("pickup_code") val pickup_code: String? = null,
    @SerializedName("delivery_code") val delivery_code: String? = null,
    @SerializedName("pickup_state") val pickup_state: String? = null,
    @SerializedName("fulfiller_profile") val fulfiller_profile: FulfillerPublicProfile? = null,
    @SerializedName("history") val history: List<StatusHistoryItem>? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("data") val data: OrderDetailsResponse? = null,

    // Return Info (v4.2)
    val vendor_allows: Boolean? = null,
    val vendor_window: Int? = null,
    val kitchen_allows: Boolean? = null,
    val kitchen_window: Int? = null,
    val return_status: String? = null,
    val created_at: String? = null,
    val scheduled_at: String? = null
)

data class ReturnRequest(
    val reason: String,
    val evidence_urls: List<String> = emptyList()
)

data class ProcessReturnRequest(
    val status: String, // APPROVED, DECLINED
    val merchant_notes: String? = null,
    val delivery_fee_payer: String = "CUSTOMER" // CUSTOMER, MERCHANT
)

data class ReturnResponse(
    val id: String,
    val order_id: Int,
    val status: String,
    val reason: String,
    val customer_name: String? = null,
    val item_description: String? = null,
    val item_price: Double? = null,
    val created_at: String
)

data class ReturnListResponse(
    val success: Boolean,
    val data: List<ReturnResponse>
)

data class FulfillerStats(
    val lifetime_earnings: Double = 0.0,
    val mtd_earnings: Double = 0.0,
    val completion_rate: Int = 100,
    val total_completed: Int = 0,
    val earnings_trend: List<Map<String, Any>> = emptyList(),
    val peak_active: Boolean = false,
    val peak_bonus: Double = 0.0
)

data class FulfillerProfileResponse(
    val id: Int? = null,
    val full_name: String? = null,
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
    val rating_avg: Double? = null,
    val rating_count: Int? = null,
    val gender: String? = null,
    val date_of_birth: String? = null,
    val home_address: String? = null,
    val bank_name: String? = null,
    val account_number: String? = null,
    val bank_code: String? = null,
    val account_name: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val current_streak_days: Int = 0,
    val stats: FulfillerStats? = null,
    @SerializedName("data") val data: FulfillerProfileResponse? = null
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
    val gender: String? = null,
    val date_of_birth: String? = null,
    val home_address: String? = null,
    val vehicle_details: VehicleDetails? = null,
    val bank_name: String? = null,
    val account_number: String? = null,
    val bank_code: String? = null,
    val account_name: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null
)

data class UserProfileResponse(
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val kyc_status: String? = null,
    val bank_name: String? = null,
    val account_number: String? = null,
    val bank_code: String? = null,
    val account_name: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null
)

data class FulfillerStatusRequest(
    val online_status: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val current_state: String? = null
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
    val expires_at: String? = null,
    val distance_km: Double? = null,
    val collect_on_delivery_amount: Double? = null
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
    val resolution_requested: String, // handoff, cancel_with_waiver_request
    val severity: String = "MEDIUM" // LOW, MEDIUM, HIGH
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
    val pending_balance: Double? = null,
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
    val billing_type: String? = null,
    val status: String? = null,
    val daily_spend_limit: Double? = null,
    val monthly_spend_limit: Double? = null
)

data class CreateCorporateRequest(
    val company_name: String,
    val billing_email: String,
    val billing_type: String = "prepaid_wallet",
    val cac_number: String? = null,
    val business_address: String? = null
)

data class CorporateStaff(
    val id: Int? = null,
    val full_name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val daily_spend_limit: Double = 0.0,
    val monthly_spend_limit: Double = 0.0,
    val created_at: String? = null
)

data class WalletBalance(
    val balance: Double = 0.0,
    val pending_balance: Double = 0.0
)

data class CorporateDashboardData(
    val account: CorporateAccount,
    val wallet: WalletBalance,
    val stats: CorporateStats,
    val top_users: List<UserSpend>
)

data class CorporateStats(
    val total_orders: Int = 0,
    val total_spend: Double = 0.0,
    val active_staff: Int = 0
)

data class UserSpend(
    val full_name: String,
    val spend: Double
)

data class CorporateDashboardResponse(
    val success: Boolean,
    val data: CorporateDashboardData
)

data class CorporateStaffResponse(
    val success: Boolean,
    val data: List<CorporateStaff>
)

data class CorporateAccountsResponse(
    val success: Boolean,
    val data: List<CorporateAccount>
)

data class Bank(
    val name: String,
    val code: String,
    val slug: String? = null
)

data class BankResponse(
    val status: Boolean,
    val message: String? = null,
    val data: List<Bank>
)

data class AccountResolutionResponse(
    val status: Boolean,
    val message: String? = null,
    val data: AccountDetails? = null
)

data class AccountDetails(
    val account_number: String,
    val account_name: String,
    val bank_id: Int? = null
)

data class PromoValidationResponse(
    val promo_id: String? = null,
    val discount_type: String? = null, // fixed, percentage
    val value: Double? = null,
    val message: String? = null
)

data class GrowthStats(
    val referral_code: String? = null,
    val total_points: Int = 0,
    val referral_count: Int = 0,
    val total_orders_completed: Int = 0
)

data class GrowthStatsResponse(
    val success: Boolean,
    val data: GrowthStats
)

data class ReferralItem(
    val full_name: String,
    val status: String,
    val created_at: String,
    val rewarded_at: String? = null
)

data class ReferralHistoryResponse(
    val success: Boolean,
    val data: List<ReferralItem>
)

data class RedeemPointsRequest(
    val points: Int
)

data class AnalyticsTrendItem(
    val period: String,
    val volume: Int,
    val net_revenue: Double
)

data class BestSellerItem(
    val name: String,
    val units: Int,
    val revenue: Double
)

data class PeakTimeItem(
    val day: Int,
    val hour: Int,
    val volume: Int
)

data class RetentionStats(
    val total_unique_customers: Int,
    val repeat_customer_rate: Double
)

data class MerchantAnalyticsData(
    val range: String,
    val trend: List<AnalyticsTrendItem>,
    val best_sellers: List<BestSellerItem>,
    val retention: RetentionStats,
    val peak_times: List<PeakTimeItem>
)

data class MerchantAnalyticsResponse(
    val success: Boolean,
    val data: MerchantAnalyticsData
)

data class ProductUpdateRequest(
    val name: String? = null,
    val price: Double? = null,
    val stock_quantity: Int? = null,
    val description: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val nafdac_number: String? = null,
    val photo_url: String? = null,
    val active: Boolean? = null
)

data class MenuItemUpdateRequest(
    val name: String? = null,
    val price: Double? = null,
    val description: String? = null,
    val category: String? = null,
    val photo_url: String? = null,
    val prep_time_minutes: Int? = null,
    val modifiers: List<Map<String, Any>>? = null,
    val available: Boolean? = null
)

data class MerchantProfile(
    val id: String = "",
    val business_name: String = "",
    val status: String = "pending",
    val type: String = "vendor", // vendor, kitchen
    val accepts_cod: Boolean = true,
    val allows_returns: Boolean = false,
    val return_window_days: Int = 7,
    val return_policy_text: String? = null,
    val store_slug: String? = null
)

data class SetupMerchantRequest(
    val business_name: String,
    val category: String,
    val address: String,
    val cac_number: String? = null,
    val nafdac_number: String? = null,
    val bank_name: String,
    val account_number: String,
    val accepts_cod: Boolean = true
)

data class SetupMerchantResponse(
    val success: Boolean? = false,
    val message: String? = null
)

data class SetupFleetRequest(
    val business_name: String,
    val cac_number: String,
    val address: String,
    val fleet_size: Int,
    val vehicle_types: List<String>,
    val cities: List<String>
)

data class FleetPartnerDashboardResponse(
    val success: Boolean,
    val data: FleetDashboardData? = null
)

data class FleetDashboardData(
    val profile: FleetProfile,
    val fulfillers: List<FulfillerProfileResponse>,
    val stats: FleetStats
)

data class FleetProfile(
    val id: Int,
    val business_name: String,
    val status: String,
    val commission_override: Double? = null,
    val has_overflow_priority: Boolean = false
)

data class FleetStats(
    val total_active_missions: Int = 0,
    val total_fleet_volume: Double = 0.0
)

data class MerchantProfileResponse(
    val success: Boolean,
    val data: MerchantProfile? = null
)

data class MerchantBatch(
    val id: String,
    val name: String? = null,
    val status: String,
    val total_orders: Int = 0,
    val processed_orders: Int = 0,
    val created_at: String
)

data class MerchantBatchesResponse(
    val success: Boolean,
    val data: List<MerchantBatch>
)

data class BatchDetails(
    val id: String,
    val name: String? = null,
    val status: String,
    val total_orders: Int = 0,
    val processed_orders: Int = 0,
    val created_at: String,
    val orders: List<OrderDetailsResponse>
)

data class BatchDetailsResponse(
    val success: Boolean,
    val data: BatchDetails
)

data class BulkOrderMission(
    val recipient_name: String,
    val recipient_phone: String,
    val item_description: String,
    val pickup_address: String,
    val delivery_address: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val delivery_lat: Double,
    val delivery_lng: Double,
    val total_fare: Double
)

data class BulkOrderRequest(
    val batch_name: String? = null,
    val orders: List<BulkOrderMission>
)

data class Product(
    val id: String,
    val vendor_id: Int? = null,
    val kitchen_id: String? = null,
    val merchant_type: String? = "vendor", // vendor, kitchen
    val name: String,
    val price: Double,
    val stock_quantity: Int = 0,
    val description: String? = null,
    val category: String? = null,
    val photo_url: String? = null,
    val unit: String? = null,
    val nafdac_number: String? = null,
    val active: Boolean = true,
    val created_at: String = ""
)

data class MerchantDashboardData(
    val sales: List<OrderDetailsResponse> = emptyList(),
    val products: List<Product> = emptyList(),
    val batches: List<MerchantBatch> = emptyList()
)

data class MerchantDashboardResponse(
    val success: Boolean,
    val data: MerchantDashboardData
)

data class VendorDetails(
    val id: String,
    val business_name: String,
    val status: String,
    val city: String,
    val description: String?,
    val products: List<Product> = emptyList()
)

data class VendorDetailsResponse(
    val success: Boolean,
    val data: VendorDetails? = null
)

data class MenuItem(
    val id: String,
    val kitchen_id: String,
    val name: String,
    val price: Double,
    val available: Boolean,
    val description: String? = null,
    val photo_url: String? = null,
    val category: String? = null,
    val prep_time_minutes: Int = 30
)

data class KitchenDetails(
    val id: String,
    val business_name: String,
    val status: String,
    val city: String,
    val cuisine_type: String?,
    val description: String?,
    val menu: List<MenuItem> = emptyList()
)

data class KitchenDetailsResponse(
    val success: Boolean,
    val data: KitchenDetails? = null
)

data class DiscoveryItem(
    val id: String,
    val name: String,
    val price: Double,
    val photo_url: String?,
    val category: String,
    val description: String?,
    val vendor_name: String,
    val vendor_id: String = "",
    val item_type: String = "product", // product, meal
    val city: String? = null,
    val pickup_address: String? = null,
    val distance_km: Double? = null,
    val created_at: String = "",
    val accepts_cod: Boolean = true,
    val is_open: Boolean = true,
    val next_open_time: String? = null,
    val operating_hours: Map<String, Map<String, String>>? = null
)

data class DiscoveryResponse(
    val success: Boolean,
    val data: List<DiscoveryItem> = emptyList()
)

data class CommerceOrderRequest(
    val items: List<Map<String, Any>>? = null,
    val item_id: String? = null,
    val item_type: String? = null, // product, meal
    val delivery_address: String,
    val lat: Double,
    val lng: Double,
    val city: String? = "Port Harcourt",
    val payment_method: String, // CARD, COD, WALLETPAY
    val scheduled_at: String? = null,
    val promo_id: String? = null
)

data class PaymentInitializationRequest(
    val amount: Double, // in Naira
    val email: String,
    val quote_id: String? = null,
    val item_price: Double? = null,
    val delivery_fee: Double? = null,
    val platform_fee_amount: Double? = null,
    val fee_payer: String? = null,
    val seller_phone: String? = null,
    val promo_id: String? = null,
    val pickup_state: String? = null,
    val metadata: Map<String, String>? = null
)

data class PaymentInitializationResponse(
    val authorization_url: String? = null,
    val access_code: String? = null,
    val reference: String? = null,
    val order_id: String? = null
)

interface ApiService {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("api/v1/auth/resend-otp")
    suspend fun resendOtp(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/request-email-otp")
    suspend fun requestEmailOtp(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/fcm-token")
    suspend fun updateFCMToken(@Body request: Map<String, String>): AuthResponse

    @GET("api/v1/legal/config")
    suspend fun getLegalConfig(): Map<String, String>

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

    @GET("api/v1/fulfillers/demand-heatmap")
    suspend fun getDemandHeatmap(): Map<String, Any>

    @GET("api/v1/fulfillers/banks")
    suspend fun getBanks(): BankResponse

    @POST("api/v1/fulfillers/resolve-account")
    suspend fun resolveAccount(@Body request: Map<String, String>): AccountResolutionResponse

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

    @POST("api/v1/orders/{id}/acknowledge")
    suspend fun acknowledgeOrder(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>

    @POST("api/v1/orders/{id}/timeout-choice")
    suspend fun resolveTimeout(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>

    @POST("api/v1/orders/{id}/confirm-receipt")
    suspend fun confirmReceipt(@retrofit2.http.Path("id") id: String): Map<String, Any>

    @POST("api/v1/orders/{id}/request-consent")
    suspend fun requestConsent(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>

    @POST("api/v1/orders/{id}/fail")
    suspend fun failDelivery(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>

    @POST("api/v1/orders/{id}/dispute")
    suspend fun reportProblem(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>

    @GET("api/v1/orders/{orderId}/messages")
    suspend fun getOrderMessages(@retrofit2.http.Path("orderId") orderId: String): List<ChatMessage>

    @POST("api/v1/orders/{id}/cancel")
    suspend fun cancelOrder(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): AuthResponse

    @PATCH("api/v1/orders/{id}/reschedule")
    suspend fun rescheduleOrder(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): AuthResponse

    @POST("api/v1/orders/{id}/incident")
    suspend fun fileIncident(@retrofit2.http.Path("id") id: String, @Body request: IncidentRequest): AuthResponse

    @GET("api/v1/orders/me/queue-candidates")
    suspend fun getQueueCandidates(): List<OfferResponse>

    @POST("api/v1/orders/{id}/queue/claim")
    suspend fun claimQueueOrder(@retrofit2.http.Path("id") id: String): AuthResponse

    @POST("api/v1/orders/{id}/rate")
    suspend fun rateCustomer(@retrofit2.http.Path("id") id: String, @Body request: RatingRequest): OrderResponse

    @POST("api/v1/orders/{id}/rate-fulfiller")
    suspend fun rateFulfiller(@retrofit2.http.Path("id") id: String, @Body request: RatingRequest): Map<String, Any>

    @GET("api/v1/addresses")
    suspend fun getSavedAddresses(): SavedAddressesResponse

    @GET("api/v1/addresses/landmark-suggestions")
    suspend fun getLandmarkSuggestions(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int? = 500
    ): LandmarkSuggestionResponse

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

    @POST("api/v1/support/ask")
    suspend fun askAiAssistant(@Body request: Map<String, String>): Map<String, Any>

    @GET("api/v1/support/conversations/{id}/messages")
    suspend fun getSupportMessages(@retrofit2.http.Path("id") id: String): List<ChatMessage>

    @GET("api/v1/support/kb")
    suspend fun getKnowledgeBase(@retrofit2.http.Query("group") group: String? = null): List<KnowledgeBaseArticle>

    @GET("api/v1/support/kb/{articleId}")
    suspend fun getArticleById(@retrofit2.http.Path("articleId") articleId: String): KnowledgeBaseArticle

    @GET("api/v1/wallets/me")
    suspend fun getWalletInfo(): WalletResponse

    @POST("api/v1/wallets/topup")
    suspend fun initializeTopup(@Body request: Map<String, Double>): PaymentInitializationResponse

    @GET("api/v1/growth/stats")
    suspend fun getGrowthStats(): GrowthStatsResponse

    @POST("api/v1/growth/redeem")
    suspend fun redeemPoints(@Body request: RedeemPointsRequest): AuthResponse

    @GET("api/v1/growth/referrals")
    suspend fun getReferralHistory(): ReferralHistoryResponse

    @POST("api/v1/withdrawals")
    suspend fun requestWithdrawal(@Body request: WithdrawalRequest): AuthResponse

    @POST("api/v1/fulfillers/kyc/start")
    suspend fun startKycSession(@Body request: Map<String, String> = mapOf("provider" to "prembly")): KycSessionResponse

    @POST("api/v1/corporate/setup")
    suspend fun setupCorporateProfile(@Body request: CreateCorporateRequest): AuthResponse

    @GET("api/v1/corporate/dashboard")
    suspend fun getCorporateDashboard(): CorporateDashboardResponse

    @GET("api/v1/corporate/staff")
    suspend fun getCorporateStaff(): CorporateStaffResponse

    @POST("api/v1/corporate/staff")
    suspend fun addStaffMember(@Body request: Map<String, String>): AuthResponse

    @GET("api/v1/corporate/my-authorizations")
    suspend fun getMyAuthorizations(): CorporateAccountsResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: Map<String, String>): AuthResponse

    @GET("api/v1/merchants/dashboard")
    suspend fun getMerchantDashboard(): MerchantDashboardResponse

    @GET("api/v1/merchants/analytics")
    suspend fun getMerchantAnalytics(@retrofit2.http.Query("range") range: String): MerchantAnalyticsResponse

    @GET("api/v1/merchants/profile")
    suspend fun getMerchantProfile(): MerchantProfileResponse

    @GET("api/v1/merchants/slug/{slug}")
    suspend fun getMerchantBySlug(@retrofit2.http.Path("slug") slug: String): MerchantProfileResponse

    @POST("api/v1/merchants/setup")
    suspend fun setupMerchantProfile(@Body request: SetupMerchantRequest): SetupMerchantResponse

    // Fleet Partner Methods (v4.1)
    @POST("api/v1/fleet-partners/setup")
    suspend fun setupFleetProfile(@Body request: SetupFleetRequest): SetupMerchantResponse

    @GET("api/v1/fleet-partners/dashboard")
    suspend fun getFleetDashboard(): FleetPartnerDashboardResponse

    @GET("api/v1/fleet-partners/invite-code")
    suspend fun getFleetInviteCode(): Map<String, String>

    // Expansion & Nationwide (v4.2)
    @POST("api/v1/expansion/waitlist")
    suspend fun joinWaitlist(@Body request: JoinWaitlistRequest): Map<String, Any>

    @GET("api/v1/expansion/cities/{name}/rules")
    suspend fun getCityRules(@retrofit2.http.Path("name") name: String): Map<String, Any>
    
    @PATCH("api/v1/merchants/settings")
    suspend fun updateMerchantSettings(@Body request: Map<String, Any>): AuthResponse
    
    @POST("api/v1/orders/{orderId}/return")
    suspend fun requestReturn(@retrofit2.http.Path("orderId") orderId: String, @Body request: ReturnRequest): Map<String, Any>

    @GET("api/v1/merchants/orders")
    suspend fun getMerchantIncomingOrders(): List<OrderDetailsResponse>
    
    @PATCH("api/v1/merchants/orders/{id}/status")
    suspend fun updateMerchantOrderStatus(@retrofit2.http.Path("id") id: String, @Body request: Map<String, String>): AuthResponse

    @GET("api/v1/merchants/returns")
    suspend fun getMerchantReturnRequests(): ReturnListResponse

    @POST("api/v1/merchants/returns/{returnId}/process")
    suspend fun processReturnRequest(@retrofit2.http.Path("returnId") returnId: String, @Body request: ProcessReturnRequest): Map<String, Any>

    @POST("api/v1/merchants/returns/{returnId}/confirm-receipt")
    suspend fun confirmReturnReceipt(@retrofit2.http.Path("returnId") returnId: String): Map<String, Any>

    @GET("api/v1/merchants/my-batches")
    suspend fun getMerchantBatches(): MerchantBatchesResponse

    @GET("api/v1/merchants/my-batches/{id}")
    suspend fun getBatchDetails(@retrofit2.http.Path("id") id: String): BatchDetailsResponse

    @GET("api/v1/merchants/coupons")
    suspend fun getMerchantCoupons(): Map<String, Any>

    @POST("api/v1/merchants/coupons")
    suspend fun createMerchantCoupon(@Body request: Map<String, Any>): Map<String, Any>

    @POST("api/v1/merchants/orders/bulk-session")
    suspend fun createBulkOrders(@Body request: BulkOrderRequest): Map<String, Any>

    @GET("api/v1/marketplace/vendors/{id}")
    suspend fun getVendorDetails(@retrofit2.http.Path("id") id: String): VendorDetailsResponse

    @GET("api/v1/kitchens/{id}")
    suspend fun getKitchenDetails(@retrofit2.http.Path("id") id: String): KitchenDetailsResponse

    @POST("api/v1/marketplace/products")
    suspend fun addProduct(@Body request: Map<String, Any>): Map<String, Any>

    @PATCH("api/v1/marketplace/products/{id}")
    suspend fun updateProduct(@retrofit2.http.Path("id") id: String, @Body request: ProductUpdateRequest): Map<String, Any>

    @DELETE("api/v1/marketplace/products/{id}")
    suspend fun deleteProduct(@retrofit2.http.Path("id") id: String): AuthResponse

    @POST("api/v1/kitchens/menu-items")
    suspend fun addMenuItem(@Body request: Map<String, Any>): Map<String, Any>

    @PATCH("api/v1/kitchens/menu-items/{id}")
    suspend fun updateMenuItem(@retrofit2.http.Path("id") id: String, @Body request: MenuItemUpdateRequest): Map<String, Any>

    @DELETE("api/v1/kitchens/menu-items/{id}")
    suspend fun deleteMenuItem(@retrofit2.http.Path("id") id: String): AuthResponse

    @GET("api/v1/commerce/discovery")
    suspend fun getDiscovery(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("category") category: String? = null,
        @Query("query") query: String? = null,
        @Query("item_type") item_type: String? = null,
        @Query("vendor_id") vendor_id: String? = null
    ): DiscoveryResponse

    @POST("api/v1/commerce/checkout/initialize")
    suspend fun initializeCommerceOrder(@Body request: CommerceOrderRequest): PaymentInitializationResponse

    @GET("api/v1/settings/profile")
    suspend fun getUserProfile(): UserProfileResponse

    @PATCH("api/v1/settings/profile")
    suspend fun updateUserProfile(@Body request: ProfileUpdateRequest): AuthResponse

    @Multipart
    @POST("api/v1/fulfillers/kyc/document")
    suspend fun uploadKycDocument(
        @Part("doc_type") type: okhttp3.RequestBody,
        @Part("expiry_date") expiry: okhttp3.RequestBody?,
        @Part file: okhttp3.MultipartBody.Part
    ): AuthResponse

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/confirm-password")
    suspend fun confirmPassword(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/delete-account")
    suspend fun deleteAccount(): AuthResponse

    // Emergency (v4.3)
    @POST("api/v1/orders/sos")
    suspend fun triggerSOS(@Body request: SOSRequest): Map<String, Any>

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
