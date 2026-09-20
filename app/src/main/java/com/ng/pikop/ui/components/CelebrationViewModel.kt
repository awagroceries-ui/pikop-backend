package com.ng.pikop.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CelebrationViewModel : ViewModel() {
    private val _showCelebration = MutableStateFlow(false)
    val showCelebration = _showCelebration.asStateFlow()

    fun trigger() {
        viewModelScope.launch {
            _showCelebration.value = true
        }
    }

    fun dismiss() {
        _showCelebration.value = false
    }
}
