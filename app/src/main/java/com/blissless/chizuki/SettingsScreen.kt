package com.blissless.chizuki

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
            .background(Color(0xFF0A0E14))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // ---- Updates section ----
        SettingsSectionHeader("Updates")
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Check for Updates on Start", color = Color(0xFFE5E7EB), fontSize = 15.sp)
                    Switch(
                        checked = checkUpdatesOnStart,
                        onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF64B5F6),
                            uncheckedThumbColor = Color(0xFF6B6B6B),
                            uncheckedTrackColor = Color(0xFF1E1E1E)
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
                        Text("Current Version", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                        Text(
                            currentVersion.ifBlank { "unknown" },
                            color = Color(0xFFE5E7EB),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    when {
                        isCheckingUpdates -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF64B5F6)
                            )
                        }
                        pendingUpdate != null -> {
                            TextButton(onClick = { viewModel.openReleasesPage() }) {
                                Text(
                                    "v${pendingUpdate!!.tagName.removePrefix("v")} available →",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            TextButton(onClick = { viewModel.checkForUpdatesManually() }) {
                                Text("Check now", color = Color(0xFF64B5F6))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ---- Extensions section (nav-item that opens a dialog) ----
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

        // ---- About section ----
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

    // ---- Extensions selection dialog (matches Oni's pattern) ----
    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            containerColor = Color(0xFF141414),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "Installed Extensions",
                    color = Color(0xFFE5E7EB),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text(
                            "No extensions installed.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Install an APK whose label starts with \"Chizuki: \".",
                            color = Color(0xFF6B6B6B),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "Tap an extension to select it:",
                            color = Color(0xFF9CA3AF),
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
                                            if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f)
                                            else Color(0xFF64B5F6).copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF10B981) else Color(0xFF64B5F6),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ext.label.removePrefix("Chizuki: "),
                                        color = if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        ext.packageName,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        "Active",
                                        color = Color(0xFF10B981),
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
                                Text("Clear selection", color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionsDialog = false }) {
                    Text("Close", color = Color(0xFF64B5F6))
                }
            }
        )
    }
}

// ---------- Helper composables (modeled on Oni) ----------

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF64B5F6),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141414)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = Color(0xFF1E1E1E)
    )
}

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color = Color(0xFF64B5F6),
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
            Text(title, color = Color(0xFFE5E7EB), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
        if (trailing != null) trailing()
        else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF6B6B6B),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
