package com.jegly.pages.feature.library.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jegly.pages.core.navigation.Navigator
import com.jegly.pages.feature.library.api.navigation.LibraryNavKey
import com.jegly.pages.feature.library.impl.LibraryScreen
import com.jegly.pages.feature.reader.api.navigation.ReaderNavKey

fun EntryProviderScope<NavKey>.libraryEntry(
    navigator: Navigator,
    onSettingsClick: () -> Unit,
) {
    entry<LibraryNavKey> {
        LibraryScreen(
            onBookClick = { uri -> navigator.navigate(ReaderNavKey(uri)) },
            onSettingsClick = onSettingsClick,
        )
    }
}
