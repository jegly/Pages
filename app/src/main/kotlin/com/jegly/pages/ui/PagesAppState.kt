package com.jegly.pages.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.jegly.pages.core.navigation.NavigationState
import com.jegly.pages.core.navigation.rememberNavigationState
import com.jegly.pages.feature.library.api.navigation.LibraryNavKey
import com.jegly.pages.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberPagesAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): PagesAppState {
    val navigationState = rememberNavigationState(LibraryNavKey, TOP_LEVEL_NAV_ITEMS.keys)
    return remember(navigationState, coroutineScope) {
        PagesAppState(navigationState = navigationState)
    }
}

@Stable
class PagesAppState(
    val navigationState: NavigationState,
)
