package com.ng.pikop.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.R

@Composable
fun UserTypeSelectionScreen(onRoleSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0E11) // Dark background matching reference design
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Pikop Logo (Kept 100% untouched)
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Heading: "How do you want to use Pikop?"
            val headingText = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                    append("How do you want to\nuse ")
                }
                withStyle(SpanStyle(color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                    append("Pikop?")
                }
            }

            Text(
                text = headingText,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Choose your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Row 1: Send (Left) & Earn (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RoleCard(
                    title = "Send",
                    subtitle = "Request a delivery",
                    iconRes = R.drawable.role_icon_send,
                    themeColor = Color(0xFF10B981),
                    containerColor = Color(0xFF042017),
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CUSTOMER") }
                )
                RoleCard(
                    title = "Earn",
                    subtitle = "Deliver & earn",
                    iconRes = R.drawable.role_icon_earn,
                    themeColor = Color(0xFFF59E0B),
                    containerColor = Color(0xFF261A04),
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FULFILLER") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Sell (Full Width)
            RoleCard(
                title = "Sell",
                subtitle = "List products & grow",
                iconRes = R.drawable.role_icon_sell,
                themeColor = Color(0xFFF97316),
                containerColor = Color(0xFF241004),
                isFullWidth = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRoleSelected("MERCHANT") }
            )

            // Organizational Grouping Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF27272A),
                    thickness = 1.dp
                )
                Text(
                    text = "FOR ORGANIZATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF27272A),
                    thickness = 1.dp
                )
            }

            // Row 3: Fleet Partner (Left) & Business Account (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RoleCard(
                    title = "Fleet Partner",
                    subtitle = "Manage your fleet",
                    iconRes = R.drawable.role_icon_fleet_partner,
                    themeColor = Color(0xFF10B981),
                    containerColor = Color(0xFF042017),
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FLEET_PARTNER") }
                )
                RoleCard(
                    title = "Business Account",
                    subtitle = "Team billing & controls",
                    iconRes = R.drawable.role_icon_business_account,
                    themeColor = Color(0xFF0284C7),
                    containerColor = Color(0xFF041829),
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CORPORATE") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer: Log In Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onRoleSelected("LOGIN") }
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
                Text(
                    text = "Log in",
                    color = Color(0xFF00E676),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trust Signal
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your data is safe with us",
                    color = Color(0xFF71717A),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    themeColor: Color,
    containerColor: Color,
    isFullWidth: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(if (isFullWidth) 110.dp else 190.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, themeColor.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isFullWidth) {
                // Horizontal Layout for Sell
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    // Circular Arrow Affordance
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
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
            } else {
                // Vertical Layout for 2-column cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Bottom-Right Circular Arrow Affordance
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = iconColor.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2
            )
        }
    }
}
