package com.example.emergency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.screen.map.InteractiveMap
import com.example.emergency.ui.state.MapFilter
import com.example.emergency.ui.state.MapPoi
import com.example.emergency.ui.state.MapUiState
import com.example.emergency.ui.theme.EmergencyTheme

/**
 * Map sub-screen. Hosts the [SubScreenTopBar] (so the back-button contract
 * with [com.example.emergency.ui.nav.AppNavHost] keeps working) and an
 * interactive MapLibre map underneath.
 *
 * The placeholder filter chips and POI list that used to live here are gone:
 * the real map ships with clustering and tap-to-route, which supersedes them.
 * The legacy [onFilterClick] / [onPoiClick] callbacks are kept as no-ops so
 * the public signature stays stable for the navigation graph and any
 * existing previews.
 */
@Composable
fun MapScreen(
    state: MapUiState,
    onBack: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onFilterClick: (MapFilter) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onPoiClick: (MapPoi) -> Unit = {},
) {
    val colors = EmergencyTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = state.title, onBack = onBack)
        InteractiveMap(modifier = Modifier.fillMaxSize())
    }
}
