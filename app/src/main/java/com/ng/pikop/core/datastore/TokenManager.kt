package com.ng.pikop.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pikop_preferences")

class TokenManager(private val context: Context) {

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

    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY]
        }

    val userEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_EMAIL_KEY]
        }

    val userName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY]
        }

    val userPhone: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_PHONE_KEY]
        }

    val userRole: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ROLE_KEY]
        }

    val isVerified: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_VERIFIED_KEY] ?: false
        }

    val referralCode: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[REFERRAL_CODE_KEY]
        }

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
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
