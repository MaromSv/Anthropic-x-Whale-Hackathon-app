package com.example.emergency.ui.state

enum class ToolId { FIRST_AID, ABC_CHECK, MAP, GET_OUT }

enum class CrowdDensity { NONE, LOW, MEDIUM, HIGH }

data class ToolTile(
    val id: ToolId,
    val title: String,
    val subtitle: String,
)

data class MapSummary(
    val title: String,
    val sub: String,
    val density: CrowdDensity,
)

data class HomeUiState(
    val greeting: String,
    val subtitle: String,
    val mapSummary: MapSummary,
    val tools: List<ToolTile>,
)

val SampleHomeUiState = HomeUiState(
    greeting = "What's happening?",
    subtitle = "Tell me, or jump straight to a tool below.",
    mapSummary = MapSummary(
        title = "Around you",
        sub = "3 medical \u00B7 4 exits \u00B7 2 shelters",
        density = CrowdDensity.HIGH,
    ),
    tools = listOf(
        ToolTile(ToolId.FIRST_AID, "First aid", "CPR \u00B7 bleeding \u00B7 choking"),
        ToolTile(ToolId.ABC_CHECK, "ABC check", "Airway \u00B7 Breathing \u00B7 Circulation"),
        ToolTile(ToolId.MAP, "Map", "Exits \u00B7 medics \u00B7 shelters"),
        ToolTile(ToolId.GET_OUT, "Get out", "Crowd density \u00B7 safe route"),
    ),
)
