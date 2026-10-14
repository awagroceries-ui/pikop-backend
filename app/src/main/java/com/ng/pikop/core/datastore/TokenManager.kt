package com.ng.pikop.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private val Context.dataStore by preferencesDataStore(name = "pikop_preferences")

class TokenManager(private val context: Context) {

    // Sign-in tokens live ONLY in encrypted storage (Android Keystore). They are mirrored in two
    // process-wide flows so every TokenManager instance sees the same value instantly.
    private val sharedPrefs: SharedPreferences = securePrefs(context.applicationContext)

    private val _sessionEvents = MutableSharedFlow<SessionEvent>()
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    enum class SessionEvent { EXPIRED }

    suspend fun emitSessionExpired() {
        _sessionEvents.emit(SessionEvent.EXPIRED)
    }

    companion object {
        private const val SECURE_FILE = "pikop_secure_prefs"
        private const val LEGACY_FILE = "pikop_sync_prefs"
        @Volatile private var secure: SharedPreferences? = null
        private val accessFlow = MutableStateFlow<String?>(null)
        private val refreshFlow = MutableStateFlow<String?>(null)

        private fun openEncrypted(ctx: Context): SharedPreferences {
            val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(
                ctx, SECURE_FILE, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        @Synchronized
        private fun securePrefs(ctx: Context): SharedPreferences {
            secure?.let { return it }
            val prefs = try {
                openEncrypted(ctx)
            } catch (e: Exception) {
                // Keystore key lost or file unreadable (can happen after a restore or OS reset).
                // Start clean: the person simply signs in again.
                try { ctx.deleteSharedPreferences(SECURE_FILE) } catch (_: Exception) { }
                openEncrypted(ctx)
            }
            // One-time move of tokens saved by older app versions in plain text.
            val legacy = ctx.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE)
            if (legacy.contains("access_token")) {
                if (!prefs.contains("access_token")) {
                    prefs.edit()
                        .putString("access_token", legacy.getString("access_token", null))
                        .putString("refresh_token", legacy.getString("refresh_token", null))
                        .apply()
                }
                legacy.edit().clear().apply()
            }
            accessFlow.value = prefs.getString("access_token", null)
            refreshFlow.value = prefs.getString("refresh_token", null)
            secure = prefs
            return prefs
        }

        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val IS_VERIFIED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_verified")
        private val REFERRAL_CODE_KEY = stringPreferencesKey("referral_code")
        private val KYC_STATUS_KEY = stringPreferencesKey("kyc_status")
    }

    val accessToken: Flow<String?> = accessFlow
    val refreshToken: Flow<String?> = refreshFlow
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val userPhone: Flow<String?> = context.dataStore.data.map { it[USER_PHONE_KEY] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE_KEY] }
    val isVerified: Flow<Boolean> = context.dataStore.data.map { it[IS_VERIFIED_KEY] ?: false }
    val referralCode: Flow<String?> = context.dataStore.data.map { it[REFERRAL_CODE_KEY] }
    val kycStatus: Flow<String?> = context.dataStore.data.map { it[KYC_STATUS_KEY] }

    /**
     * Synchronous access for Network Interceptors to avoid DataStore async race conditions.
     */
    fun getAccessTokenSync(): String? = accessFlow.value
    fun getRefreshTokenSync(): String? = refreshFlow.value

    suspend fun saveTokens(
        accessToken: String, 
        refreshToken: String, 
        userId: String? = null,
        email: String, 
        role: String, 
        name: String? = null,
        phone: String? = null,
        isVerified: Boolean = false,
        referralCode: String? = null,
        kycStatus: String? = null
    ) {
        // Tokens go to encrypted storage only (never to the plain DataStore).
        sharedPrefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }
        accessFlow.value = accessToken
        refreshFlow.value = refreshToken

        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY) // clear any plain-text copy left by older versions
            preferences.remove(REFRESH_TOKEN_KEY)
            if (userId != null) preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ROLE_KEY] = role
            preferences[IS_VERIFIED_KEY] = isVerified
            if (name != null) preferences[USER_NAME_KEY] = name
            if (phone != null) preferences[USER_PHONE_KEY] = phone
            if (referralCode != null) preferences[REFERRAL_CODE_KEY] = referralCode
            if (kycStatus != null) preferences[KYC_STATUS_KEY] = kycStatus
        }
    }

    suspend fun clearTokens() {
        sharedPrefs.edit().clear().apply()
        accessFlow.value = null
        refreshFlow.value = null
        context.dataStore.edit { it.clear() }
    }
}
