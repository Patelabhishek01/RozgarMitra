package com.rozgarmitra.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rozgarmitra.app.ui.theme.*

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Int = 16
) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Verified Badge",
        tint = Color(0xFF38BDF8),
        modifier = modifier.size(size.dp)
    )
}

@Composable
fun OfflineBanner(isOffline: Boolean) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarningRose.copy(alpha = 0.9f))
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = "Offline",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "You are currently offline. Using cached data.",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    text: String = "Please wait..."
) {
    if (isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, BorderStrokeColor),
                tonalElevation = 12.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.size(160.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryIndigo,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = PrimaryIndigo,
        contentColor = Color.White,
        disabledContainerColor = DarkSurfaceVariant,
        disabledContentColor = TextMuted
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    stars: Int = 5,
    starSize: Int = 32,
    clickable: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..stars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Star $i",
                tint = if (i <= rating) AmberGold else BorderStrokeColor,
                modifier = Modifier
                    .size(starSize.dp)
                    .padding(2.dp)
                    .then(
                        if (clickable) {
                            Modifier.clickable {
                                onRatingChanged(i.toFloat())
                            }
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderStrokeColor),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String? = null,
    titleColor: Color = TextPrimary,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (titleColor == WarningRose) WarningRose.copy(alpha = 0.15f) else PrimaryIndigo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (titleColor == WarningRose) WarningRose else PrimaryIndigo, 
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        if (badgeCount > 0) {
            Badge(
                containerColor = PrimaryIndigo,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(badgeCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

data class RozgarNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val badgeCount: Int = 0,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RozgarBottomNavBar(
    items: List<RozgarNavItem>,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        border = BorderStroke(1.dp, BorderStrokeColor)
    ) {
        NavigationBar(
            tonalElevation = 0.dp,
            containerColor = DarkSurface,
            windowInsets = windowInsets
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = {
                        if (item.badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = PrimaryIndigo) {
                                        Text(
                                            text = item.badgeCount.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = PrimaryIndigo
                    )
                )
            }
        }
    }
}

