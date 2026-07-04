package com.blissless.chizuki

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 40.dp, bottom = 12.dp)
            .navigationBarsPadding(),
        shape = MaterialTheme.shapes.extraLarge,
        color = DarkCard,
        border = BorderStroke(0.5.dp, GlassStroke),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.index == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) BlueAccent else Color.Transparent,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else SilverDark,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(if (isSelected) 1.8f else 1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
