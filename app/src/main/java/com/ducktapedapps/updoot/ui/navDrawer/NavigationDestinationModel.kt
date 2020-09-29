package com.ducktapedapps.updoot.ui.navDrawer

import androidx.annotation.DrawableRes
import com.ducktapedapps.updoot.R
import com.ducktapedapps.updoot.ui.navDrawer.NavigationDestination.*

sealed class NavigationDestination(val title: String, @DrawableRes val icon: Int, val isUserSpecific: Boolean) {
    object Search : NavigationDestination("Search", R.drawable.ic_search_24dp, false)
    object Explore : NavigationDestination("Explore", R.drawable.ic_explore_24dp, false)
    object CreatePost : NavigationDestination("Create Post", R.drawable.ic_baseline_edit_24, true)
    object Inbox : NavigationDestination("Inbox", R.drawable.ic_baseline_inbox_24, true)
    object History : NavigationDestination("History", R.drawable.ic_baseline_history_24, false)
    object Exit : NavigationDestination("Exit", R.drawable.ic_baseline_exit_to_app_24, false)
}

val AllNavigationEntries = listOf(
        Search,
        Explore,
        CreatePost,
        Inbox,
        History,
        Exit
)