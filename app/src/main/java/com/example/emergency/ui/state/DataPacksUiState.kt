package com.example.emergency.ui.state

enum class DataPackStatus { INSTALLED, UPDATE_AVAILABLE, NOT_INSTALLED, DOWNLOADING, RECOMMENDED }

data class PackContent(
    val count: String,
    val label: String,
)

data class DataPack(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,
    val status: DataPackStatus,
    val updatedLabel: String? = null,
    val issuer: String? = null,
    val whenLabel: String? = null,
    val priceLabel: String? = null,
    val summary: String? = null,
    val badge: String? = null,
    val paletteId: String? = null,
    val contents: List<PackContent> = emptyList(),
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
            id = "kingsday-ams",
            name = "King's Day \u00B7 Amsterdam",
            description = "Events, crowd zones, exits, medics",
            sizeLabel = "24 MB",
            status = DataPackStatus.RECOMMENDED,
            updatedLabel = "Apr 27, 2026",
            issuer = "Gemeente Amsterdam",
            whenLabel = "Apr 27, 2026",
            priceLabel = "Free",
            summary = "Official emergency data for the city-wide festivities. Includes 87 medical posts, 43 portaloo clusters, all marked safe-zones, road-block plan, and live crowd-feed seed data.",
            badge = "Recommended for today",
            paletteId = "kingsday-ams",
            contents = listOf(
                PackContent("87", "Medical posts"),
                PackContent("312", "Public toilets"),
                PackContent("54", "Drinking water"),
                PackContent("12", "Calm zones"),
                PackContent("31", "Marked exits"),
                PackContent("28", "Transit re-routes"),
            ),
        ),
        DataPack(
            id = "ams-base",
            name = "Amsterdam \u00B7 base layer",
            description = "Hospitals, pharmacies, police, embassies",
            sizeLabel = "186 MB",
            status = DataPackStatus.INSTALLED,
            updatedLabel = "Updated Mar 2026",
            issuer = "Gemeente Amsterdam",
            whenLabel = "Updated Mar 2026",
            priceLabel = "Free",
            summary = "Year-round map and emergency services for central Amsterdam. Hospitals, police, fire, AEDs, 24/7 pharmacies.",
            paletteId = "ams-base",
            contents = listOf(
                PackContent("142", "Hospitals & GPs"),
                PackContent("1208", "Public AEDs"),
                PackContent("67", "Police & shelter"),
            ),
        ),
        DataPack(
            id = "flooding-nl",
            name = "Flooding \u00B7 Netherlands",
            description = "Evacuation routes, dyke breach plans",
            sizeLabel = "32 MB",
            status = DataPackStatus.NOT_INSTALLED,
            updatedLabel = "Updated Mar 2026",
            issuer = "Rijksoverheid",
            whenLabel = "Updated Mar 2026",
            priceLabel = "Free",
            summary = "National flood-risk overlay. Evacuation routes, dyke breach plans, designated high-ground shelters, and the latest water-board advisories.",
            paletteId = "flooding-nl",
            contents = listOf(
                PackContent("64", "Evacuation routes"),
                PackContent("41", "High-ground shelters"),
                PackContent("18", "Field hospitals"),
                PackContent("12", "Safety guides"),
            ),
        ),
        DataPack(
            id = "power-outage-nl",
            name = "Power outage \u00B7 Netherlands",
            description = "Battery guidance, heated rooms, water points",
            sizeLabel = "14 MB",
            status = DataPackStatus.NOT_INSTALLED,
            updatedLabel = "Updated Feb 2026",
            issuer = "Rijksoverheid",
            whenLabel = "Updated Feb 2026",
            priceLabel = "Free",
            summary = "What to do when the grid goes down. Battery-conservation guidance, locations of designated heated rooms (warmtekamers), water distribution points, and emergency radio frequencies.",
            paletteId = "power-outage-nl",
            contents = listOf(
                PackContent("86", "Heated rooms"),
                PackContent("24", "Water points"),
                PackContent("9", "Survival guides"),
                PackContent("6", "Radio frequencies"),
            ),
        ),
        DataPack(
            id = "rotterdam-base",
            name = "Rotterdam \u00B7 base layer",
            description = "Hospitals, AEDs, police, harbour exits",
            sizeLabel = "142 MB",
            status = DataPackStatus.NOT_INSTALLED,
            updatedLabel = "Updated Jan 2026",
            issuer = "Gemeente Rotterdam",
            whenLabel = "Updated Jan 2026",
            priceLabel = "Free",
            summary = "Year-round emergency layer for Rotterdam. Hospitals, AEDs, police, harbour-area exits.",
            paletteId = "rotterdam-base",
            contents = listOf(
                PackContent("98", "Hospitals & GPs"),
                PackContent("743", "Public AEDs"),
            ),
        ),
    ),
)
