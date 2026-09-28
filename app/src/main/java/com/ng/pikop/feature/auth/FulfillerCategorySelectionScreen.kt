package com.ng.pikop.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.R

@Composable
fun FulfillerCategorySelectionScreen(
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val surfaceBg = if (isDark) Color(0xFF070A0D) else MaterialTheme.colorScheme.background
    val titleTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val greenAccent = if (isDark) Color(0xFF00E676) else Color(0xFF008751)
    val greyTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Circular Back Button and Pikop Logo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = titleTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Pikop Logo (Kept 100% untouched and identical to other screens)
                    Image(
                        painter = painterResource(id = R.drawable.pikop_logo),
                        contentDescription = "Pikop Logo",
                        modifier = Modifier.size(80.dp)
                    )

                    // Spacer for header symmetry
                    Spacer(modifier = Modifier.width(42.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Heading Area
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "BECOME A",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = greyTextColor,
                        letterSpacing = 2.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val headingText = buildAnnotatedString {
                        withStyle(SpanStyle(color = titleTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)) {
                            append("Pikop ")
                        }
                        withStyle(SpanStyle(color = greenAccent, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)) {
                            append("Agent")
                        }
                    }

                    Text(
                        text = headingText,
                        lineHeight = 36.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose your mobility category and start earning today.",
                        fontSize = 14.sp,
                        color = greyTextColor,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Row 1: Foot Agent / Cyclist (Left) & Rider (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CategoryCard(
                        title = "Foot Agent /\nCyclist",
                        description = "Short distance,\nfast deliveries.",
                        iconRes = R.drawable.agent_icon_foot_agent_cyclist,
                        themeColor = if (isDark) Color(0xFF00E676) else Color(0xFF008751),
                        containerColor = if (isDark) Color(0xFF042017) else Color(0xFFECFDF5),
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected("FOOT_AGENT") }
                    )

                    CategoryCard(
                        title = "Rider",
                        description = "Motorcycle for\nmedium parcels.",
                        iconRes = R.drawable.agent_icon_rider,
                        themeColor = if (isDark) Color(0xFFFFC107) else Color(0xFFD97706),
                        containerColor = if (isDark) Color(0xFF261A04) else Color(0xFFFFFBEB),
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategorySelected("RIDER") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 2: Driver (Full Width)
                CategoryCard(
                    title = "Driver",
                    description = "Car or Van for\nlarge items.",
                    iconRes = R.drawable.agent_icon_driver,
                    themeColor = if (isDark) Color(0xFF00B0FF) else Color(0xFF0284C7),
                    containerColor = if (isDark) Color(0xFF041829) else Color(0xFFF0F9FF),
                    isFullWidth = true,
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onCategorySelected("DRIVER") }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Footer Tagline
                Text(
                    text = "More ways to move. More opportunities for you.",
                    fontSize = 12.sp,
                    color = greyTextColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Glowing Accent Curve
                val curveGlowColor = greenAccent
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val path = Path().apply {
                        moveTo(0f, height)
                        quadraticTo(width / 2f, 0f, width, height)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                curveGlowColor.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        style = Stroke(width = 2.5f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    description: String,
    iconRes: Int,
    themeColor: Color,
    containerColor: Color,
    isFullWidth: Boolean = false,
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardTitleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val cardSubtitleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.2.dp, themeColor.copy(alpha = if (isDark) 0.65f else 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isFullWidth) {
                // Horizontal Layout for Driver
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(110.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardTitleColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description.replace("\n", " "),
                            fontSize = 12.sp,
                            color = cardSubtitleColor,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Circular Arrow Affordance
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.6f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // Vertical Layout for 2-Column Cards (Foot Agent/Cyclist & Rider)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = title,
                            modifier = Modifier.size(125.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cardTitleColor,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = description,
                            fontSize = 11.sp,
                            color = cardSubtitleColor,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Bottom-Right Circular Arrow Affordance
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.6f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
