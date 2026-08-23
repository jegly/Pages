package com.jegly.pages.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.jegly.pages.R
import com.jegly.pages.core.designsystem.icon.PagesIcons
import com.jegly.pages.feature.bookmarks.api.navigation.BookmarksNavKey
import com.jegly.pages.feature.library.api.navigation.LibraryNavKey
import com.jegly.pages.feature.search.api.navigation.SearchNavKey
import com.jegly.pages.feature.library.api.R as libraryR
import com.jegly.pages.feature.bookmarks.api.R as bookmarksR
import com.jegly.pages.feature.search.api.R as searchR

data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

val LIBRARY = TopLevelNavItem(
    selectedIcon = PagesIcons.Bookmarks,
    unselectedIcon = PagesIcons.BookmarksBorder,
    iconTextId = libraryR.string.feature_library_api_title,
    titleTextId = libraryR.string.feature_library_api_title,
)

val BOOKMARKS = TopLevelNavItem(
    selectedIcon = PagesIcons.Bookmark,
    unselectedIcon = PagesIcons.BookmarkBorder,
    iconTextId = bookmarksR.string.feature_bookmarks_api_title,
    titleTextId = bookmarksR.string.feature_bookmarks_api_title,
)

val SEARCH = TopLevelNavItem(
    selectedIcon = PagesIcons.Search,
    unselectedIcon = PagesIcons.Search,
    iconTextId = searchR.string.feature_search_api_title,
    titleTextId = searchR.string.feature_search_api_title,
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    LibraryNavKey to LIBRARY,
    BookmarksNavKey to BOOKMARKS,
    SearchNavKey to SEARCH,
)
