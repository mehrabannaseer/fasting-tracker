package com.axisphysique.axisfasting.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axisphysique.axisfasting.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToTips: () -> Unit,
    themeViewModel: ThemeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    
    // These should ideally be in a ViewModel to persist
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress") },
            text = { Text("Are you sure you want to delete all fasting history, water, and weight data? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsHeader("Preferences")
                SettingsSwitchItem("Enable Notifications", "Receive fast completion alerts", Icons.Default.Notifications, notificationsEnabled) { notificationsEnabled = it }
                SettingsSwitchItem("Notification Sound", "Play a sound when fast ends", Icons.AutoMirrored.Filled.VolumeUp, soundEnabled) { soundEnabled = it }
                SettingsSwitchItem("Dark Mode", "Use dark theme", Icons.Default.DarkMode, isDarkMode) { themeViewModel.toggleDarkMode(it) }
            }

            item {
                SettingsHeader("Account & Data")
                SettingsClickItem("Export Data", "Download your fasting history", Icons.Default.Download) {}
                SettingsClickItem("Reset Progress", "Clear all fasting records", Icons.Default.DeleteForever, color = MaterialTheme.colorScheme.error) {
                    showResetDialog = true
                }
            }

            item {
                SettingsHeader("Support")
                SettingsClickItem("Fasting Guide", "Learn more about fasting benefits", Icons.AutoMirrored.Filled.MenuBook) {
                    onNavigateToTips()
                }
                SettingsClickItem("Rate the App", "Support us on the Play Store", Icons.Default.Star) {}
                SettingsClickItem("About Axis Fasting", "Version 1.0.0", Icons.Default.Info) {}
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = color) },
        modifier = Modifier.clickable { onClick() }
    )
}
