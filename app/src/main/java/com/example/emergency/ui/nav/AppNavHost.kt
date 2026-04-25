package com.example.emergency.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emergency.ui.screen.AbcCheckScreen
import com.example.emergency.ui.screen.ConversationsScreen
import com.example.emergency.ui.screen.DataPacksScreen
import com.example.emergency.ui.screen.FirstAidScreen
import com.example.emergency.ui.screen.GetOutScreen
import com.example.emergency.ui.screen.HomeShell
import com.example.emergency.ui.screen.MapScreen
import com.example.emergency.ui.screen.PersonalInfoScreen
import com.example.emergency.ui.screen.SettingsScreen
import com.example.emergency.ui.state.SampleAbcCheckUiState
import com.example.emergency.ui.state.SampleConversationsUiState
import com.example.emergency.ui.state.SampleDataPacksUiState
import com.example.emergency.ui.state.SampleDrawerUiState
import com.example.emergency.ui.state.SampleFirstAidUiState
import com.example.emergency.ui.state.SampleGetOutUiState
import com.example.emergency.ui.state.SampleHomeUiState
import com.example.emergency.ui.state.SampleMapUiState
import com.example.emergency.ui.state.SamplePersonalInfoUiState
import com.example.emergency.ui.state.SampleSettingsUiState

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
                    navController.navigate(id.toRoute().path)
                },
                onDrawerItemClick = { id ->
                    navController.navigate(id.toRoute().path)
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
        composable(Route.Conversations.path) {
            ConversationsScreen(
                state = SampleConversationsUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.Map.path) {
            MapScreen(
                state = SampleMapUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.FirstAid.path) {
            FirstAidScreen(
                state = SampleFirstAidUiState,
                onBack = { navController.popBackStack() },
                onAbcCheckClick = { navController.navigate(Route.AbcCheck.path) },
            )
        }
        composable(Route.AbcCheck.path) {
            AbcCheckScreen(
                state = SampleAbcCheckUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.GetOut.path) {
            GetOutScreen(
                state = SampleGetOutUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.Settings.path) {
            SettingsScreen(
                state = SampleSettingsUiState,
                onBack = { navController.popBackStack() },
                onPersonalInfoClick = { navController.navigate(Route.PersonalInfo.path) },
            )
        }
    }
}
