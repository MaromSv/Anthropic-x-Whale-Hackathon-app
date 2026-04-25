package com.example.emergency.ui.state

enum class PoiKind { MEDICAL, EXIT, SHELTER, POLICE, FIRST_AID }

enum class MapFilter { ALL, MEDICAL, EXITS, SHELTERS, CROWD }

data class MapPoi(
    val id: String,
    val name: String,
    val kind: PoiKind,
    val kindLabel: String,
    val distanceLabel: String,
    val bearingLabel: String,
)

data class MapUiState(
    val title: String,
    val locationLabel: String,
    val density: CrowdDensity,
    val densityLabel: String,
    val filters: List<MapFilter>,
    val selectedFilter: MapFilter,
    val pois: List<MapPoi>,
)

val SampleMapUiState = MapUiState(
    title = "Map",
    locationLabel = "Vondelpark",
    density = CrowdDensity.HIGH,
    densityLabel = "High density",
    filters = listOf(
        MapFilter.ALL,
        MapFilter.MEDICAL,
        MapFilter.EXITS,
        MapFilter.SHELTERS,
        MapFilter.CROWD,
    ),
    selectedFilter = MapFilter.ALL,
    pois = listOf(
        MapPoi(
            id = "medical-tent-1",
            name = "Medical tent \u2013 Roemer Visscherstraat",
            kind = PoiKind.MEDICAL,
            kindLabel = "Medical tent",
            distanceLabel = "80 m",
            bearingLabel = "NE",
        ),
        MapPoi(
            id = "exit-west",
            name = "West exit \u2013 Amstelveenseweg",
            kind = PoiKind.EXIT,
            kindLabel = "Park exit",
            distanceLabel = "210 m",
            bearingLabel = "W",
        ),
        MapPoi(
            id = "shelter-pavilion",
            name = "Pavilion shelter",
            kind = PoiKind.SHELTER,
            kindLabel = "Indoor shelter",
            distanceLabel = "340 m",
            bearingLabel = "S",
        ),
        MapPoi(
            id = "police-post",
            name = "Police post \u2013 Stadhouderskade",
            kind = PoiKind.POLICE,
            kindLabel = "Police",
            distanceLabel = "420 m",
            bearingLabel = "SE",
        ),
        MapPoi(
            id = "first-aid-volunteers",
            name = "Red Cross volunteers",
            kind = PoiKind.FIRST_AID,
            kindLabel = "First aid",
            distanceLabel = "150 m",
            bearingLabel = "N",
        ),
    ),
)
