package com.blissless.chizuki

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val checkUpdatesOnStart by viewModel.checkUpdatesOnStart.collectAsState()
    val extensions by viewModel.installedExtensions.collectAsState()
    val selectedAuthority by viewModel.selectedExtensionAuthority.collectAsState()
    val pendingUpdate by viewModel.pendingUpdate.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val currentVersion by viewModel.currentVersionName.collectAsState()

    var showExtensionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.discoverExtensions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(24.dp))

        SettingsSectionHeader("Updates")
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Check for Updates on Start", color = Silver, fontSize = 15.sp)
                    Switch(
                        checked = checkUpdatesOnStart,
                        onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BlueAccent,
                            checkedTrackColor = BlueAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = SilverDark,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                SettingsDivider()
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Version", color = SilverDark, fontSize = 13.sp)
                        Text(
                            currentVersion.ifBlank { "unknown" },
                            color = Silver,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    when {
                        isCheckingUpdates -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BlueAccent
                            )
                        }
                        pendingUpdate != null -> {
                            TextButton(onClick = { viewModel.openReleasesPage() }) {
                                Text(
                                    "v${pendingUpdate!!.tagName.removePrefix("v")} available →",
                                    color = StatusCompleted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            TextButton(onClick = { viewModel.checkForUpdatesManually() }) {
                                Text("Check now", color = BlueAccent)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionHeader("Extensions")
        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Widgets,
                title = "Installed Extensions",
                subtitle = if (extensions.isEmpty()) {
                    "Tap to discover"
                } else {
                    val selected = extensions.find { it.authority == selectedAuthority }
                    if (selected != null) {
                        selected.label.removePrefix("Chizuki: ")
                    } else {
                        "${extensions.size} extension(s) found"
                    }
                },
                onClick = {
                    viewModel.discoverExtensions()
                    showExtensionsDialog = true
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionHeader("About")
        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Info,
                title = "Chizuki",
                subtitle = "Version ${currentVersion.ifBlank { "1.0.0" }}",
                onClick = { viewModel.openReleasesPage() }
            )
        }

        Spacer(Modifier.height(40.dp))
    }

    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Installed Extensions",
                    color = Silver,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text(
                            "No extensions installed.",
                            color = SilverDark,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Install an APK whose label starts with \"Chizuki: \".",
                            color = SilverDark.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "Tap an extension to select it:",
                            color = SilverDark,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        extensions.forEachIndexed { index, ext ->
                            val isSelected = ext.authority == selectedAuthority
                            if (index > 0) SettingsDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectExtension(
                                            if (isSelected) null else ext.authority
                                        )
                                        showExtensionsDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) StatusCompleted.copy(alpha = 0.2f)
                                            else BlueAccent.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (isSelected) StatusCompleted else BlueAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ext.label.removePrefix("Chizuki: "),
                                        color = if (isSelected) StatusCompleted else Silver,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        ext.packageName,
                                        color = SilverDark,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        "Active",
                                        color = StatusCompleted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (selectedAuthority != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.selectExtension(null)
                                    showExtensionsDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear selection", color = StatusDropped)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionsDialog = false }) {
                    Text("Close", color = BlueAccent)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = BlueAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(0.5.dp, GlassStroke)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = GlassStroke
    )
}

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color = BlueAccent,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Silver, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = SilverDark, fontSize = 12.sp)
        }
        if (trailing != null) trailing()
        else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = SilverDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
