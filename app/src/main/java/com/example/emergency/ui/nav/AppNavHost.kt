package com.example.emergency.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emergency.ui.screen.DataPacksScreen
import com.example.emergency.ui.screen.HomeShell
import com.example.emergency.ui.screen.PersonalInfoScreen
import com.example.emergency.ui.state.SampleDataPacksUiState
import com.example.emergency.ui.state.SampleDrawerUiState
import com.example.emergency.ui.state.SampleHomeUiState
import com.example.emergency.ui.state.SamplePersonalInfoUiState

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Home.path,
    ) {
        composable(Route.Home.path) {
            HomeShell(
                homeState = SampleHomeUiState,
                drawerState = SampleDrawerUiState,
                onToolClick = { id ->
                    id.toRoute()?.let { navController.navigate(it.path) }
                },
                onDrawerItemClick = { id ->
                    id.toRoute()?.let { navController.navigate(it.path) }
                },
            )
        }
        composable(Route.DataPacks.path) {
            DataPacksScreen(
                state = SampleDataPacksUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.PersonalInfo.path) {
            PersonalInfoScreen(
                state = SamplePersonalInfoUiState,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
