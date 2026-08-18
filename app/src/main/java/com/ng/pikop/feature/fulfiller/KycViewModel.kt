package com.ng.pikop.feature.fulfiller

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ng.pikop.core.kyc.KycManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.FulfillerProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KycViewModel @Inject constructor(
    private val apiService: ApiService,
    private val kycManager: KycManager
) : ViewModel() {

    private val _profile = MutableStateFlow<FulfillerProfileResponse?>(null)
    val profile: StateFlow<FulfillerProfileResponse?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLaunching = MutableStateFlow(false)
    val isLaunching: StateFlow<Boolean> = _isLaunching.asStateFlow()

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val res = apiService.getFulfillerProfile()
                _profile.value = res
            } catch (e: Exception) {
                android.util.Log.e("KycViewModel", "Profile refresh failed", e)
            }
        }
    }

    fun startVerification(context: Context, email: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLaunching.value = true
            try {
                // Request a session from backend if needed, or use a unique reference
                val referenceId = "pikop_verify_${System.currentTimeMillis()}"
                
                kycManager.startVerification(
                    context = context,
                    email = email,
                    referenceId = referenceId,
                    onSuccess = { result ->
                        _isLaunching.value = false
                        onComplete()
                    },
                    onError = { error ->
                        _isLaunching.value = false
                        // Handle error
                    },
                    onClose = {
                        _isLaunching.value = false
                    }
                )
            } catch (e: Exception) {
                _isLaunching.value = false
            }
        }
    }

    fun submitApplication(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.submitApplication()
                refreshProfile()
                onSuccess()
            } catch (e: Exception) {
                onError(com.ng.pikop.core.network.ErrorUtils.parseError(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
