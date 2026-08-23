package com.jegly.pages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import com.jegly.pages.MainViewModel
import com.jegly.pages.core.designsystem.component.PagesBackground
import com.jegly.pages.core.designsystem.icon.PagesIcons
import com.jegly.pages.core.navigation.Navigator
import com.jegly.pages.core.navigation.toEntries
import com.jegly.pages.feature.bookmarks.api.navigation.BookmarksNavKey
import com.jegly.pages.feature.bookmarks.impl.navigation.bookmarksEntry
import com.jegly.pages.feature.library.api.navigation.LibraryNavKey
import com.jegly.pages.feature.library.impl.navigation.libraryEntry
import com.jegly.pages.feature.reader.impl.navigation.readerEntry
import com.jegly.pages.feature.search.api.navigation.SearchNavKey
import com.jegly.pages.feature.search.impl.navigation.searchEntry
import com.jegly.pages.feature.settings.impl.SettingsDialog
import com.jegly.pages.navigation.TOP_LEVEL_NAV_ITEMS

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun PagesApp(
    appState: PagesAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateCollectionDialog by rememberSaveable { mutableStateOf(false) }
    val navigator = remember { Navigator(appState.navigationState) }
    val currentTopLevelKey = appState.navigationState.currentTopLevelKey
    val isAtTopLevel = appState.navigationState.isAtTopLevel
    val isCompact = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onCreate = { name ->
                mainViewModel.createCollection(name)
                showCreateCollectionDialog = false
            },
            onDismiss = { showCreateCollectionDialog = false },
        )
    }

    PagesBackground(modifier = modifier) {
        if (isCompact) {
            // Phone: Box layout — full edge-to-edge, nav FAB overlaid at bottom-start
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true },
            ) {
                PagesNavContent(
                    navigator = navigator,
                    appState = appState,
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isAtTopLevel)
                                Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                                )
                            else
                                Modifier
                        ),
                )

                if (isAtTopLevel) {
                    PagesNavFab(
                        currentKey = currentTopLevelKey,
                        onNavigate = { navigator.navigate(it) },
                        onCreateCollection = { showCreateCollectionDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, bottom = 16.dp),
                    )
                }
            }
        } else {
            // Tablet / large screen: NavigationRail on the left
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true },
            ) {
                if (isAtTopLevel) {
                    PagesNavigationRail(
                        currentKey = currentTopLevelKey,
                        onNavigate = { navigator.navigate(it) },
                        onSettingsClick = { showSettingsDialog = true },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    PagesNavContent(
                        navigator = navigator,
                        appState = appState,
                        onSettingsClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isAtTopLevel)
                                    Modifier.windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(
                                            WindowInsetsSides.Top + WindowInsetsSides.Right,
                                        ),
                                    )
                                else
                                    Modifier
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PagesNavContent(
    navigator: Navigator,
    appState: PagesAppState,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider {
        libraryEntry(navigator, onSettingsClick = onSettingsClick)
        bookmarksEntry(navigator)
        searchEntry(navigator)
        readerEntry(navigator)
    }
    Box(modifier = modifier) {
        NavDisplay(
            entries = appState.navigationState.toEntries(entryProvider),
            sceneStrategies = emptyList(),
            onBack = { if (appState.navigationState.canGoBack) navigator.goBack() },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PagesNavFab(
    currentKey: NavKey,
    onNavigate: (NavKey) -> Unit,
    onCreateCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = "Navigation menu",
                    modifier = Modifier.graphicsLayer { rotationZ = checkedProgress * 45f },
                )
            }
        },
        modifier = modifier,
    ) {
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onNavigate(LibraryNavKey) },
            icon = { Icon(PagesIcons.Bookmarks, contentDescription = null) },
            text = { Text("Library") },
        )
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onNavigate(BookmarksNavKey) },
            icon = { Icon(Icons.Rounded.Bookmark, contentDescription = null) },
            text = { Text("Saved") },
        )
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onNavigate(SearchNavKey) },
            icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            text = { Text("Search") },
        )
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onCreateCollection() },
            icon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
            text = { Text("New Collection") },
        )
    }
}

@Composable
private fun CreateCollectionDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PagesNavigationRail(
    currentKey: NavKey,
    onNavigate: (NavKey) -> Unit,
    onSettingsClick: () -> Unit,
) {
    NavigationRail(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        TOP_LEVEL_NAV_ITEMS.forEach { (key, item) ->
            val selected = key == currentKey
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(key) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(item.iconTextId)) },
            )
        }
    }
}
