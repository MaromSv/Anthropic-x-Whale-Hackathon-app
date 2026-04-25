package com.example.emergency.ui.state

data class SettingsUiState(
    val title: String,
    val languageLabel: String,
    val regionLabel: String,
    val showOnLockScreen: Boolean,
    val crowdAlerts: Boolean,
    val highDensityWarnings: Boolean,
    val modelName: String,
    val modelStorageLabel: String,
    val modelLastSyncLabel: String,
    val versionLabel: String,
)

val SampleSettingsUiState = SettingsUiState(
    title = "Settings",
    languageLabel = "English",
    regionLabel = "Netherlands",
    showOnLockScreen = true,
    crowdAlerts = true,
    highDensityWarnings = true,
    modelName = "Gemma 2B",
    modelStorageLabel = "1.2 GB used",
    modelLastSyncLabel = "Synced today, 09:14",
    versionLabel = "v0.4.2",
)
