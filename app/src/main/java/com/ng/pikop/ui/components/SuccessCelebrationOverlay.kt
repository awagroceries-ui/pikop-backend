package com.ng.pikop.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.dp
import com.ng.pikop.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SuccessCelebrationOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val accessibilityManager = LocalAccessibilityManager.current
    val reduceMotion = accessibilityManager?.calculateRecommendedTimeoutMillis(1000, true, true, true) != null
    // Note: Compose doesn't have a direct "reduce motion" boolean in all versions, 
    // but we can check if animations are generally enabled or use a fallback approach.
    // For this implementation, we'll assume a smooth fade-in checkmark for accessibility.

    var startAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(100)
            startAnimation = true
            delay(2500)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        if (reduceMotion) {
            // Static / Gentle Fallback
            Surface(
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = PikopGreen,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        } else {
            // Animated Burst
            val scale by animateFloatAsState(
                targetValue = if (startAnimation) 1.2f else 0.8f,
                animationSpec = tween(durationMillis = 600, easing = OvershootInterpolator(2f).toEasing()),
                label = "Scale"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "Alpha"
            )

            Box(contentAlignment = Alignment.Center) {
                // Confetti Particles
                repeat(20) { index ->
                    ConfettiParticle(startAnimation, index)
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .alpha(alpha)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = PikopGreen,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiParticle(start: Boolean, index: Int) {
    val colors = listOf(PikopGreen, PikopLemonGreen, PikopGold, PikopOrange)
    val color = colors[index % colors.size]
    
    val angle = (index * 18).toDouble()
    val radius by animateFloatAsState(
        targetValue = if (start) 150f + (index * 5) else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "Radius"
    )
    
    val pAlpha by animateFloatAsState(
        targetValue = if (start) 0f else 1f,
        animationSpec = tween(durationMillis = 1500, delayMillis = 500),
        label = "PAlpha"
    )

    val x = (radius * Math.cos(Math.toRadians(angle))).dp
    val y = (radius * Math.sin(Math.toRadians(angle))).dp

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(if (index % 2 == 0) 12.dp else 8.dp)
            .alpha(pAlpha)
            .background(color, if (index % 3 == 0) CircleShape else MaterialTheme.shapes.extraSmall)
    )
}

// Helper to convert Android Interpolator to Compose Easing
fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }

class OvershootInterpolator(private val tension: Float) : android.view.animation.Interpolator {
    override fun getInterpolation(t: Float): Float {
        var time = t - 1.0f
        return time * time * ((tension + 1) * time + tension) + 1.0f
    }
}
