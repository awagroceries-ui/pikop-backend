package com.ng.pikop.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "pikop_preferences")

class TokenManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("pikop_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val IS_VERIFIED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_verified")
        private val REFERRAL_CODE_KEY = stringPreferencesKey("referral_code")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val userPhone: Flow<String?> = context.dataStore.data.map { it[USER_PHONE_KEY] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE_KEY] }
    val isVerified: Flow<Boolean> = context.dataStore.data.map { it[IS_VERIFIED_KEY] ?: false }
    val referralCode: Flow<String?> = context.dataStore.data.map { it[REFERRAL_CODE_KEY] }

    /**
     * Synchronous access for Network Interceptors to avoid DataStore async race conditions.
     */
    fun getAccessTokenSync(): String? = sharedPrefs.getString("access_token", null)
    fun getRefreshTokenSync(): String? = sharedPrefs.getString("refresh_token", null)

    suspend fun saveTokens(
        accessToken: String, 
        refreshToken: String, 
        userId: String? = null,
        email: String, 
        role: String, 
        name: String? = null,
        phone: String? = null,
        isVerified: Boolean = false,
        referralCode: String? = null
    ) {
        // Parallel Sync save to SharedPrefs
        sharedPrefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }

        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            if (userId != null) preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ROLE_KEY] = role
            preferences[IS_VERIFIED_KEY] = isVerified
            if (name != null) preferences[USER_NAME_KEY] = name
            if (phone != null) preferences[USER_PHONE_KEY] = phone
            if (referralCode != null) preferences[REFERRAL_CODE_KEY] = referralCode
        }
    }

    suspend fun clearTokens() {
        sharedPrefs.edit().clear().apply()
        context.dataStore.edit { it.clear() }
    }
}
