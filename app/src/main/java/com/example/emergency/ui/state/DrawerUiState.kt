package com.example.emergency.ui.state

enum class DrawerItemId {
    CONVERSATIONS,
    MAP,
    FIRST_AID,
    DATA_PACKS,
    SETTINGS,
    PERSONAL_INFO,
}

sealed interface DrawerEntry {
    data class Item(
        val id: DrawerItemId,
        val label: String,
        val sub: String? = null,
        val badge: String? = null,
    ) : DrawerEntry

    data object Divider : DrawerEntry
}

data class DrawerUiState(
    val title: String,
    val statusLabel: String,
    val statusOk: Boolean,
    val entries: List<DrawerEntry>,
    val footer: String,
)

val SampleDrawerUiState = DrawerUiState(
    title = "Mark",
    statusLabel = "On-device \u00B7 ready offline",
    statusOk = true,
    entries = listOf(
        DrawerEntry.Item(DrawerItemId.CONVERSATIONS, "Conversations", sub = "3 saved"),
        DrawerEntry.Item(DrawerItemId.MAP, "Map"),
        DrawerEntry.Item(DrawerItemId.FIRST_AID, "First aid"),
        DrawerEntry.Divider,
        DrawerEntry.Item(
            DrawerItemId.DATA_PACKS,
            "Data packs",
            sub = "1 update \u00B7 King's Day pack ready",
            badge = "NEW",
        ),
        DrawerEntry.Item(DrawerItemId.SETTINGS, "Settings"),
        DrawerEntry.Item(
            DrawerItemId.PERSONAL_INFO,
            "Personal information",
            sub = "Allergies \u00B7 meds \u00B7 contacts",
        ),
    ),
    footer = "Works fully offline \u00B7 No data leaves your device \u00B7 v0.4.2",
)
