package com.jegly.pages.feature.library.impl

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.Collection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var importMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val singleFilePicker = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.addBookFromUri(uri)
    }

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        uris.forEach { viewModel.addBookFromUri(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = importMenuExpanded,
                modifier = Modifier.navigationBarsPadding(),
                button = {
                    ToggleFloatingActionButton(
                        checked = importMenuExpanded,
                        onCheckedChange = { importMenuExpanded = it },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Import",
                            modifier = Modifier.graphicsLayer { rotationZ = checkedProgress * 45f },
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        singleFilePicker.launch(arrayOf("application/pdf"))
                    },
                    icon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                    text = { Text("PDF") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        singleFilePicker.launch(arrayOf("application/epub+zip"))
                    },
                    icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    text = { Text("EPUB") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        singleFilePicker.launch(arrayOf("text/plain"))
                    },
                    icon = { Icon(Icons.Rounded.Article, contentDescription = null) },
                    text = { Text("TXT") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        singleFilePicker.launch(arrayOf("application/x-mobipocket-ebook", "application/mobi", "*/*"))
                    },
                    icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    text = { Text("MOBI") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        singleFilePicker.launch(arrayOf("application/vnd.comicbook+zip", "application/x-cbz", "*/*"))
                    },
                    icon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                    text = { Text("CBZ") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        importMenuExpanded = false
                        multiFilePicker.launch(arrayOf("*/*"))
                    },
                    icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                    text = { Text("Browse") },
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is LibraryUiState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is LibraryUiState.Success -> LibraryContent(
                books = state.books,
                collections = collections,
                searchQuery = state.searchQuery,
                viewType = viewType,
                sortOrder = sortOrder,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onCollectionSelected = viewModel::selectCollection,
                onCycleSortOrder = viewModel::cycleSortOrder,
                onToggleViewType = viewModel::toggleViewType,
                onSettingsClick = onSettingsClick,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                onBookClick = { book ->
                    if (prefs.pdfRequireAuth && book.format == BookFormat.PDF) {
                        (context as? FragmentActivity)?.let { activity ->
                            showPdfAuth(activity) { onBookClick(book.uri) }
                        } ?: onBookClick(book.uri)
                    } else {
                        onBookClick(book.uri)
                    }
                },
                onBookDelete = viewModel::removeBook,
                onBookMoveToCollection = viewModel::moveBookToCollection,
                onBookShare = { uri ->
                    viewModel.shareBook(uri)?.let { context.startActivity(it) }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryContent(
    books: List<Book>,
    collections: List<Collection>,
    searchQuery: String,
    viewType: LibraryViewType,
    sortOrder: SortOrder,
    onSearchQueryChanged: (String) -> Unit,
    onCollectionSelected: (Long?) -> Unit,
    onCycleSortOrder: () -> Unit,
    onToggleViewType: () -> Unit,
    onSettingsClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onBookDelete: (String) -> Unit,
    onBookMoveToCollection: (String, Long?) -> Unit,
    onBookShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var contextBook by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionPickerBookUri by rememberSaveable { mutableStateOf<String?>(null) }

    // Bottom sheet: book context menu
    contextBook?.let { bookUri ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { contextBook = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            BookContextMenu(
                onDelete = {
                    onBookDelete(bookUri)
                    contextBook = null
                },
                onMoveToCollection = {
                    collectionPickerBookUri = bookUri
                    contextBook = null
                },
                onShare = {
                    onBookShare(bookUri)
                    contextBook = null
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    // Dialog: pick collection to move book to
    collectionPickerBookUri?.let { bookUri ->
        CollectionPickerDialog(
            collections = collections,
            onSelect = { collId ->
                onBookMoveToCollection(bookUri, collId)
                collectionPickerBookUri = null
            },
            onDismiss = { collectionPickerBookUri = null },
        )
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChanged,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Search library") },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {}
            IconButton(onClick = onCycleSortOrder) {
                Icon(Icons.Rounded.Sort, contentDescription = "Sort: ${sortOrder.name}")
            }
            IconButton(onClick = onToggleViewType) {
                Icon(
                    imageVector = if (viewType == LibraryViewType.GRID) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = if (viewType == LibraryViewType.GRID) "Switch to list" else "Switch to grid",
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }

        // Collection filter chips
        if (collections.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCollectionId == null,
                    onClick = {
                        selectedCollectionId = null
                        onCollectionSelected(null)
                    },
                    label = { Text("All") },
                )
                collections.forEach { collection ->
                    FilterChip(
                        selected = selectedCollectionId == collection.id,
                        onClick = {
                            val newId = if (selectedCollectionId == collection.id) null else collection.id
                            selectedCollectionId = newId
                            onCollectionSelected(newId)
                        },
                        label = { Text(collection.name) },
                    )
                }
            }
        }

        if (books.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No books yet.\nTap Import to get started."
                    else "No books matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (viewType == LibraryViewType.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = navBarPadding + 88.dp, // nav bar + clearance for both FABs
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(books, key = { it.uri }) { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { contextBook = book.uri },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = navBarPadding + 88.dp, // nav bar + clearance for both FABs
                ),
            ) {
                items(books, key = { it.uri }) { book ->
                    BookListItem(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { contextBook = book.uri },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        colors = CardDefaults.elevatedCardColors(),
    ) {
        Column {
            // Cover image
            if (book.coverUri != null) {
                val coverBitmap by produceState<android.graphics.Bitmap?>(
                    initialValue = null,
                    key1 = book.coverUri,
                ) {
                    value = BitmapFactory.decodeFile(book.coverUri)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    coverBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                if (book.authors.isNotBlank()) {
                    Text(
                        text = book.authors,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = book.format.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (book.progressPercent > 0f) {
                    Text(
                        text = "${(book.progressPercent * 100).toInt()}% read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = book.coverUri,
    ) {
        value = book.coverUri?.let { BitmapFactory.decodeFile(it) }
    }

    ListItem(
        headlineContent = {
            Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = buildString {
                    if (book.authors.isNotBlank()) append(book.authors)
                    if (book.authors.isNotBlank()) append("  •  ")
                    append(book.format.name)
                    if (book.progressPercent > 0f) {
                        append("  •  ${(book.progressPercent * 100).toInt()}%")
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = if (book.coverUri != null) {
            {
                Box(
                    modifier = Modifier.size(width = 48.dp, height = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    coverBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else null,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookContextMenu(
    onDelete: () -> Unit,
    onMoveToCollection: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("Move to Collection") },
            leadingContent = { Icon(Icons.Rounded.DriveFileMove, contentDescription = null) },
            modifier = Modifier.combinedClickable(onClick = onMoveToCollection),
        )
        ListItem(
            headlineContent = { Text("Export / Share") },
            leadingContent = { Icon(Icons.Rounded.Share, contentDescription = null) },
            modifier = Modifier.combinedClickable(onClick = onShare),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            },
            leadingContent = {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            modifier = Modifier.combinedClickable(onClick = onDelete),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionPickerDialog(
    collections: List<Collection>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Collection") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("None (remove from collection)") },
                    modifier = Modifier.combinedClickable(onClick = { onSelect(null) }),
                )
                HorizontalDivider()
                collections.forEach { collection ->
                    ListItem(
                        headlineContent = { Text(collection.name) },
                        modifier = Modifier.combinedClickable(onClick = { onSelect(collection.id) }),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun showPdfAuth(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = Unit
            override fun onAuthenticationFailed() = Unit
        },
    ).authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("PDF Protection")
            .setSubtitle("Authenticate to open this PDF")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build(),
    )
}
