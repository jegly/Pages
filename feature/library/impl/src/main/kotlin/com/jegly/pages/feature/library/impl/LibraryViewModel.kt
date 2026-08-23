package com.jegly.pages.feature.library.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.extractCover
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.security.EncryptedFileManager
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.Collection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

enum class LibraryViewType { GRID, LIST }

enum class SortOrder { Recent, Title, Author, Size, Format }

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val books: List<Book>, val searchQuery: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: PagesRepository,
    @ApplicationContext private val context: Context,
    private val encryptedFileManager: EncryptedFileManager,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCollectionId = MutableStateFlow<Long?>(null)

    val viewType = MutableStateFlow(LibraryViewType.GRID)
    val sortOrder = MutableStateFlow(SortOrder.Recent)

    val userPreferences = repository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.jegly.pages.core.model.UserPreferences())

    val collections: StateFlow<List<Collection>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.getAllBooks(),
        searchQuery,
        selectedCollectionId,
        sortOrder,
    ) { books, query, collId, sort ->
        val filtered = books
            .let { list ->
                if (query.isBlank()) list
                else list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.authors.contains(query, ignoreCase = true)
                }
            }
            .let { list -> if (collId != null) list.filter { it.collectionId == collId } else list }
            .let { list ->
                when (sort) {
                    SortOrder.Recent -> list.sortedByDescending { it.lastOpened ?: it.dateAdded }
                    SortOrder.Title -> list.sortedBy { it.title.lowercase() }
                    SortOrder.Author -> list.sortedBy { it.authors.lowercase() }
                    SortOrder.Size -> list.sortedByDescending { it.fileSize }
                    SortOrder.Format -> list.sortedBy { it.format.name }
                }
            }
        LibraryUiState.Success(filtered, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun selectCollection(id: Long?) {
        selectedCollectionId.value = id
    }

    fun toggleViewType() {
        viewType.update { if (it == LibraryViewType.GRID) LibraryViewType.LIST else LibraryViewType.GRID }
    }

    fun cycleSortOrder() {
        val orders = SortOrder.entries
        val currentIndex = orders.indexOf(sortOrder.value)
        sortOrder.value = orders[(currentIndex + 1) % orders.size]
    }

    fun addBookFromUri(uri: Uri) {
        if (uri.scheme != "content") return  // reject file:// and other schemes
        viewModelScope.launch {
            val shouldEncrypt = userPreferences.value.biometricLockEnabled

            // Only persist the URI permission for unencrypted books (encrypted books are copied).
            if (!shouldEncrypt) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }

            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
            var fileSize = 0L

            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameCol >= 0) displayName = cursor.getString(nameCol)
                    if (sizeCol >= 0 && !cursor.isNull(sizeCol)) fileSize = cursor.getLong(sizeCol)
                }
            }

            val format = when {
                displayName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
                displayName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
                displayName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
                displayName.endsWith(".mobi", ignoreCase = true) -> BookFormat.MOBI
                displayName.endsWith(".cbz", ignoreCase = true) -> BookFormat.CBZ
                displayName.endsWith(".cbr", ignoreCase = true) -> BookFormat.CBR
                else -> return@launch
            }

            // For encrypted imports: copy & encrypt the file into private storage.
            // The URI stored in the book record becomes the absolute path to the .enc file.
            val (bookUri, isEncrypted) = if (shouldEncrypt) {
                val encFile = encryptedFileManager.encryptFromUri(uri)
                if (encFile != null) {
                    Pair(encFile.absolutePath, true)
                } else {
                    // Fallback to unencrypted reference if encryption failed.
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    Pair(uri.toString(), false)
                }
            } else {
                Pair(uri.toString(), false)
            }

            // Extract cover from the original URI (before the original is deleted by the user).
            val coverUri = extractCover(context, uri, format)

            val book = Book(
                uri = bookUri,
                title = displayName.substringBeforeLast('.'),
                authors = "",
                coverUri = coverUri,
                format = format,
                fileSize = fileSize,
                dateAdded = Clock.System.now(),
                lastOpened = null,
                currentPosition = "0",
                progressPercent = 0f,
                collectionId = selectedCollectionId.value,
                isEncrypted = isEncrypted,
            )
            repository.upsertBook(book)
        }
    }

    fun removeBook(uri: String) {
        viewModelScope.launch {
            // If this was an encrypted copy in private storage, delete the file too.
            val book = repository.getBook(uri).first()
            if (book?.isEncrypted == true) {
                encryptedFileManager.deleteEncryptedBook(book.uri)
            }
            repository.deleteBook(uri)
        }
    }

    fun moveBookToCollection(bookUri: String, collectionId: Long?) {
        viewModelScope.launch { repository.moveBookToCollection(bookUri, collectionId) }
    }

    fun shareBook(bookUri: String): Intent? {
        val uri = Uri.parse(bookUri)
        if (uri.scheme != "content") return null  // only share content:// URIs
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Export book",
        )
    }
}
