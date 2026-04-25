package com.example.emergency.ui.nav

import com.example.emergency.ui.state.DrawerItemId
import com.example.emergency.ui.state.ToolId

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object DataPacks : Route("data_packs")
    data object PersonalInfo : Route("personal_info")
}

fun DrawerItemId.toRoute(): Route? = when (this) {
    DrawerItemId.DATA_PACKS -> Route.DataPacks
    DrawerItemId.PERSONAL_INFO -> Route.PersonalInfo
    DrawerItemId.CONVERSATIONS,
    DrawerItemId.MAP,
    DrawerItemId.FIRST_AID,
    DrawerItemId.SETTINGS -> null
}

fun ToolId.toRoute(): Route? = when (this) {
    ToolId.MAP,
    ToolId.FIRST_AID,
    ToolId.ABC_CHECK,
    ToolId.GET_OUT -> null
}
