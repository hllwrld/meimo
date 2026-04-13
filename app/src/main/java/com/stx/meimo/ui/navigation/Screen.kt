package com.stx.meimo.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ChatList : Screen("chat_list")
    data object Creator : Screen("creator")
    data object Rewards : Screen("rewards")
    data object Login : Screen("login")
    data object Profile : Screen("profile")
    data object Search : Screen("search")
    data object DebugWebView : Screen("debug_webview")

    data class RoleDetail(val roleId: Long) : Screen("role_detail/$roleId") {
        companion object {
            const val ROUTE = "role_detail/{roleId}"
            const val ARG_ROLE_ID = "roleId"
        }
    }

    data class Chat(val roleId: Long) : Screen("chat/$roleId") {
        companion object {
            const val ROUTE = "chat/{roleId}"
            const val ARG_ROLE_ID = "roleId"
        }
    }
}
