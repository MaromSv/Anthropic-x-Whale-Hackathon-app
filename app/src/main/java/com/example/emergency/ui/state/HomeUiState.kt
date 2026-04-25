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
        ToolTile(ToolId.FIRST_AID, "CPR aid", ""),
        ToolTile(ToolId.ABC_CHECK, "ABC aid", ""),
    ),
)
