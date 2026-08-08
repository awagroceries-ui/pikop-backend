package com.ng.pikop.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.ui.theme.Background
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = {
                    OvershootInterpolator(4f).getInterpolation(it)
                }
            )
        )
        delay(1500L)
        onAnimationFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pikop_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(300.dp)
                .scale(scale.value)
        )
    }
}

class OvershootInterpolator(private val tension: Float) : android.view.animation.Interpolator {
    override fun getInterpolation(input: Float): Float {
        val t = input - 1.0f
        return t * t * ((tension + 1) * t + tension) + 1.0f
    }
}
