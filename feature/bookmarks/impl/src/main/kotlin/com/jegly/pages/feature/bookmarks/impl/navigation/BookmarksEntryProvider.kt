package com.jegly.pages.feature.bookmarks.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jegly.pages.core.navigation.Navigator
import com.jegly.pages.feature.bookmarks.api.navigation.BookmarksNavKey
import com.jegly.pages.feature.bookmarks.impl.BookmarksScreen
import com.jegly.pages.feature.reader.api.navigation.ReaderNavKey

fun EntryProviderScope<NavKey>.bookmarksEntry(navigator: Navigator) {
    entry<BookmarksNavKey> {
        BookmarksScreen(
            onBookmarkClick = { bookUri, position ->
                navigator.navigate(ReaderNavKey(bookUri, position.toIntOrNull() ?: 0))
            },
        )
    }
}
