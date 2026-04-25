package com.example.emergency.ui.nav

import com.example.emergency.ui.state.DrawerItemId
import com.example.emergency.ui.state.ToolId

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object DataPacks : Route("data_packs")
    data object PersonalInfo : Route("personal_info")
    data object Conversations : Route("conversations")
    data object ChatThread : Route("chat_thread")
    data object Map : Route("map")
    data object FirstAid : Route("first_aid")
    data object AbcCheck : Route("abc_check")
    data object GetOut : Route("get_out")
    data object Settings : Route("settings")
}

fun DrawerItemId.toRoute(): Route = when (this) {
    DrawerItemId.CONVERSATIONS -> Route.Conversations
    DrawerItemId.MAP -> Route.Map
    DrawerItemId.FIRST_AID -> Route.FirstAid
    DrawerItemId.DATA_PACKS -> Route.DataPacks
    DrawerItemId.SETTINGS -> Route.Settings
    DrawerItemId.PERSONAL_INFO -> Route.PersonalInfo
}

fun ToolId.toRoute(): Route = when (this) {
    ToolId.MAP -> Route.Map
    ToolId.FIRST_AID -> Route.FirstAid
    ToolId.ABC_CHECK -> Route.AbcCheck
    ToolId.GET_OUT -> Route.GetOut
}
