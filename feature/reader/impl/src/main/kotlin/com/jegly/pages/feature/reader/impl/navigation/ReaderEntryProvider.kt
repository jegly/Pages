package com.jegly.pages.feature.reader.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jegly.pages.core.navigation.Navigator
import com.jegly.pages.feature.reader.api.navigation.ReaderNavKey
import com.jegly.pages.feature.reader.impl.ReaderScreen

fun EntryProviderScope<NavKey>.readerEntry(navigator: Navigator) {
    entry<ReaderNavKey> { key ->
        ReaderScreen(
            bookUri = key.bookUri,
            startPage = key.startPage,
            onNavigateUp = { navigator.goBack() },
        )
    }
}
