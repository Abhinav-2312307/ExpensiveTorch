package com.example.expensiveflashlight

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensiveflashlight.ui.components.FlashlightMode
import com.example.expensiveflashlight.ui.screens.FlashlightScreen
import com.example.expensiveflashlight.ui.screens.PremiumScreen
import com.example.expensiveflashlight.ui.screens.SettingsScreen
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

// Colors used locally to avoid circular dependency issues
private val GoldPrimary = Color(0xFFD4AF37)
private val GoldBright = Color(0xFFFFD700)
private val TextGray = Color(0xFF9E9E9E)
private val DeepBlack = Color(0xFF0A0A0A)
private val NavBarBg = Color(0xFF0D0D0D)
private val GlassBorder = Color(0x1AFFFFFF)

private data class BottomNavItem(
    val navKey: NavKey,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(FlashlightDest, Icons.Filled.Highlight, "FLASHLIGHT"),
    BottomNavItem(PremiumDest, Icons.Filled.WorkspacePremium, "PREMIUM"),
    BottomNavItem(SettingsDest, Icons.Filled.Settings, "SETTINGS")
)

@Composable
fun MainNavigation(
    isPremium: Boolean,
    isFlashlightOn: Boolean,
    remainingSeconds: Int,
    expiryDate: String?,
    lastPaymentId: String?,
    onToggleFlashlight: () -> Unit,
    onPayForDuration: (amountPaise: Int, durationSeconds: Int) -> Unit,
    onModeSelected: (FlashlightMode) -> Unit,
    onSubscribePremium: () -> Unit
) {
    val backStack = rememberNavBackStack(FlashlightDest)

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Content area
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<FlashlightDest> {
                    FlashlightScreen(
                        isPremium = isPremium,
                        isFlashlightOn = isFlashlightOn,
                        remainingSeconds = remainingSeconds,
                        onToggleFlashlight = onToggleFlashlight,
                        onPayForDuration = onPayForDuration,
                        onModeSelected = onModeSelected,
                        onNavigateToPremium = {
                            backStack.add(PremiumDest)
                        },
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
                entry<PremiumDest> {
                    PremiumScreen(
                        isPremium = isPremium,
                        expiryDate = expiryDate,
                        onSubscribeClick = onSubscribePremium,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
                entry<SettingsDest> {
                    SettingsScreen(
                        isPremium = isPremium,
                        expiryDate = expiryDate,
                        lastPaymentId = lastPaymentId,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
            }
        )

        // Bottom Navigation Bar
        BottomNavBar(
            currentDestination = backStack.lastOrNull() ?: FlashlightDest,
            onNavigate = { navKey ->
                // Clear back to the root and navigate
                while (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
                if (backStack.lastOrNull() != navKey) {
                    if (navKey is FlashlightDest) {
                        // Already at root or navigate to root
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    } else {
                        backStack.add(navKey)
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BottomNavBar(
    currentDestination: NavKey,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GoldPrimary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavBarBg.copy(alpha = 0.95f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentDestination::class == item.navKey::class
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) GoldBright else TextGray,
                    animationSpec = tween(300),
                    label = "navIconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) GoldPrimary else TextGray.copy(alpha = 0.6f),
                    animationSpec = tween(300),
                    label = "navLabelColor"
                )

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.navKey) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = labelColor,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                    // Active indicator
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(GoldBright, GoldPrimary)
                                    )
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
