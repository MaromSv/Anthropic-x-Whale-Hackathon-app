package com.example.emergency.ui.state

enum class DataPackStatus { INSTALLED, UPDATE_AVAILABLE, NOT_INSTALLED, DOWNLOADING }

data class DataPack(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,
    val status: DataPackStatus,
    val updatedLabel: String? = null,
)

data class DataPacksUiState(
    val title: String,
    val subtitle: String,
    val storageLabel: String,
    val packs: List<DataPack>,
)

val SampleDataPacksUiState = DataPacksUiState(
    title = "Data packs",
    subtitle = "Offline emergency content for the places you go.",
    storageLabel = "1.2 GB used \u00B7 6.8 GB free",
    packs = listOf(
        DataPack(
            id = "kingsday-2026",
            name = "King's Day 2026",
            description = "Events, crowd zones, exits, medics",
            sizeLabel = "84 MB",
            status = DataPackStatus.UPDATE_AVAILABLE,
            updatedLabel = "Update from Apr 22",
        ),
        DataPack(
            id = "ams-essentials",
            name = "Amsterdam essentials",
            description = "Hospitals, pharmacies, police, embassies",
            sizeLabel = "142 MB",
            status = DataPackStatus.INSTALLED,
            updatedLabel = "Updated Apr 18",
        ),
        DataPack(
            id = "first-aid",
            name = "First aid offline",
            description = "CPR, bleeding, choking, drowning",
            sizeLabel = "18 MB",
            status = DataPackStatus.INSTALLED,
            updatedLabel = "Updated Mar 30",
        ),
        DataPack(
            id = "pride-ams",
            name = "Pride Amsterdam",
            description = "Parade route \u00B7 safe zones \u00B7 meds tents",
            sizeLabel = "96 MB",
            status = DataPackStatus.NOT_INSTALLED,
        ),
        DataPack(
            id = "marathon-2026",
            name = "Amsterdam Marathon",
            description = "Route \u00B7 aid stations \u00B7 evacuation",
            sizeLabel = "52 MB",
            status = DataPackStatus.NOT_INSTALLED,
        ),
    ),
)
