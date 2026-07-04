package com.blissless.chizuki

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// Oni-inspired color tokens (prefixed to avoid conflicts with other files)
private val NavDarkCard = Color(0xFF1A1A1E)
private val NavGlassStroke = Color(0x1AFFFFFF)
private val NavBlueAccent = Color(0xFF3B82F6)
private val NavSilverDark = Color(0xFF9CA3AF)

private data class NavTab(
    val index: Int,
    val icon: ImageVector,
    val label: String
)

@Composable
fun ChizukiBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab(0, Icons.Filled.DateRange, "Schedule"),
        NavTab(1, Icons.Filled.Explore, "Explore"),
        NavTab(2, Icons.Filled.Home, "Home"),
        NavTab(3, Icons.Filled.Settings, "Settings")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = NavDarkCard,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(0.5.dp, NavGlassStroke)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.index == selectedTab

                    Box(
                        modifier = Modifier
                            .weight(if (isSelected) 0.67f else 0.25f)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            .height(56.dp)
                            .pointerInput(tab.index) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.any { it.pressed }) {
                                            onTabSelected(tab.index)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = NavBlueAccent,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(0.95f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tab.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = NavSilverDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
