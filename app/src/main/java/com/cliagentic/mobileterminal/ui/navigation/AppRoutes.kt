package com.cliagentic.mobileterminal.ui.navigation

sealed class AppRoute(val route: String) {
    data object Profiles : AppRoute("profiles")
    data object Settings : AppRoute("settings")
    data object Privacy : AppRoute("privacy")

    data object ProfileEditor : AppRoute("profile_editor?profileId={profileId}") {
        fun create(profileId: Long?): String {
            return if (profileId == null) {
                "profile_editor"
            } else {
                "profile_editor?profileId=$profileId"
            }
        }
    }

    data object Session : AppRoute("session/{profileId}?autoConnect={autoConnect}") {
        fun create(profileId: Long, autoConnect: Boolean = false): String {
            return "session/$profileId?autoConnect=$autoConnect"
        }
    }
}
