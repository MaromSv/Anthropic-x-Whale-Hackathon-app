package com.example.emergency.ui.state

enum class ExitStatus { CLEAR, BUSY, FULL }

data class GetOutExit(
    val id: String,
    val name: String,
    val distanceLabel: String,
    val etaLabel: String,
    val routeSummary: String,
    val status: ExitStatus,
    val crowdLabel: String,
    val recommended: Boolean = false,
)

data class GetOutUiState(
    val title: String,
    val locationLabel: String,
    val density: CrowdDensity,
    val densityLabel: String,
    val densityHint: String,
    val exits: List<GetOutExit>,
)

val SampleGetOutUiState = GetOutUiState(
    title = "Get out",
    locationLabel = "Vondelpark",
    density = CrowdDensity.HIGH,
    densityLabel = "High",
    densityHint = "Updated 12s ago \u00B7 moving south",
    exits = listOf(
        GetOutExit(
            id = "b",
            name = "Exit B (south)",
            distanceLabel = "240 m",
            etaLabel = "~5 min",
            routeSummary = "Through the food court \u00B7 low crowd",
            status = ExitStatus.CLEAR,
            crowdLabel = "Clear",
            recommended = true,
        ),
        GetOutExit(
            id = "a",
            name = "Exit A (north)",
            distanceLabel = "310 m",
            etaLabel = "~7 min",
            routeSummary = "Past main stage \u00B7 dense crowd",
            status = ExitStatus.BUSY,
            crowdLabel = "Busy",
        ),
        GetOutExit(
            id = "emergency",
            name = "Emergency exit",
            distanceLabel = "180 m",
            etaLabel = "~4 min",
            routeSummary = "Service road \u00B7 currently closed",
            status = ExitStatus.FULL,
            crowdLabel = "Closed",
        ),
    ),
)
