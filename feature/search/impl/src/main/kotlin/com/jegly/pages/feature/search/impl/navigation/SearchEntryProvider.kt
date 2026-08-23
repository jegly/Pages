package com.jegly.pages.feature.search.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jegly.pages.core.navigation.Navigator
import com.jegly.pages.feature.reader.api.navigation.ReaderNavKey
import com.jegly.pages.feature.search.api.navigation.SearchNavKey
import com.jegly.pages.feature.search.impl.SearchScreen

fun EntryProviderScope<NavKey>.searchEntry(navigator: Navigator) {
    entry<SearchNavKey> {
        SearchScreen(
            onBookClick = { uri -> navigator.navigate(ReaderNavKey(uri)) },
        )
    }
}
