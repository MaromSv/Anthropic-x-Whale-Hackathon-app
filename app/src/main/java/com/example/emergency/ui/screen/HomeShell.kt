package com.example.emergency.ui.screen

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.example.emergency.ui.screen.chat.NavDrawerSheet
import com.example.emergency.ui.state.DrawerItemId
import com.example.emergency.ui.state.DrawerUiState
import com.example.emergency.ui.state.HomeUiState
import com.example.emergency.ui.state.ToolId
import kotlinx.coroutines.launch

@Composable
fun HomeShell(
    homeState: HomeUiState,
    drawerState: DrawerUiState,
    onToolClick: (ToolId) -> Unit = {},
    onDrawerItemClick: (DrawerItemId) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onSend: (String) -> Unit = {},
    onMic: () -> Unit = {},
    onCamera: () -> Unit = {},
    onGallery: () -> Unit = {},
    pendingImages: List<String> = emptyList(),
    onRemoveImage: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val drawerController = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerController,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        drawerContent = {
            NavDrawerSheet(
                state = drawerState,
                onItemClick = { id ->
                    scope.launch { drawerController.close() }
                    onDrawerItemClick(id)
                },
            )
        },
    ) {
        ChatScreen(
            state = homeState,
            onMenuClick = { scope.launch { drawerController.open() } },
            onNewChatClick = onNewChatClick,
            onToolClick = onToolClick,
            onSend = onSend,
            onMic = onMic,
            onCamera = onCamera,
            onGallery = onGallery,
            pendingImages = pendingImages,
            onRemoveImage = onRemoveImage,
        )
    }
}
