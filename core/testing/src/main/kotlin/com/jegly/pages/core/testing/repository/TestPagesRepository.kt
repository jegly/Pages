package com.jegly.pages.core.testing.repository

import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.Bookmark
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.Collection
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.Highlight
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.Tag
import com.jegly.pages.core.model.UserPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class TestPagesRepository : PagesRepository {

    private val booksFlow = MutableSharedFlow<List<Book>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val bookmarksFlow = MutableSharedFlow<List<Bookmark>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val highlightsFlow = MutableSharedFlow<List<Highlight>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val tagsFlow = MutableSharedFlow<List<Tag>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val collectionsFlow = MutableSharedFlow<List<Collection>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val preferencesFlow = MutableSharedFlow<UserPreferences>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        booksFlow.tryEmit(emptyList())
        bookmarksFlow.tryEmit(emptyList())
        highlightsFlow.tryEmit(emptyList())
        tagsFlow.tryEmit(emptyList())
        collectionsFlow.tryEmit(emptyList())
        preferencesFlow.tryEmit(UserPreferences())
    }

    fun sendBooks(books: List<Book>) = booksFlow.tryEmit(books)

    // Books
    override fun getAllBooks(): Flow<List<Book>> = booksFlow
    override fun getBook(uri: String): Flow<Book?> = booksFlow.map { it.find { b -> b.uri == uri } }
    override fun getRecentBooks(limit: Int): Flow<List<Book>> = booksFlow.map { it.take(limit) }
    override fun searchBooks(query: String): Flow<List<Book>> =
        booksFlow.map { it.filter { b -> b.title.contains(query, ignoreCase = true) } }
    override fun getBooksByTag(tagName: String): Flow<List<Book>> =
        booksFlow.map { books ->
            val uris = tagsFlow.replayCache.firstOrNull()
                ?.filter { it.tagName == tagName }?.map { it.bookUri } ?: emptyList()
            books.filter { it.uri in uris }
        }
    override fun getBooksByCollection(collectionId: Long): Flow<List<Book>> =
        booksFlow.map { it.filter { b -> b.collectionId == collectionId } }
    override suspend fun upsertBook(book: Book) {
        val current = booksFlow.replayCache.firstOrNull() ?: emptyList()
        booksFlow.tryEmit(current.filter { it.uri != book.uri } + book)
    }
    override suspend fun upsertBooks(books: List<Book>) = books.forEach { upsertBook(it) }
    override suspend fun updateProgress(uri: String, position: String, percent: Float) {
        val current = booksFlow.replayCache.firstOrNull() ?: return
        booksFlow.tryEmit(current.map { if (it.uri == uri) it.copy(currentPosition = position, progressPercent = percent) else it })
    }
    override suspend fun moveBookToCollection(bookUri: String, collectionId: Long?) {
        val current = booksFlow.replayCache.firstOrNull() ?: return
        booksFlow.tryEmit(current.map { if (it.uri == bookUri) it.copy(collectionId = collectionId) else it })
    }
    override suspend fun deleteBook(uri: String) {
        booksFlow.tryEmit((booksFlow.replayCache.firstOrNull() ?: emptyList()).filter { it.uri != uri })
    }

    // Collections
    override fun getAllCollections(): Flow<List<Collection>> = collectionsFlow
    override suspend fun createCollection(name: String): Long {
        val current = collectionsFlow.replayCache.firstOrNull() ?: emptyList()
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        collectionsFlow.tryEmit(current + Collection(id = id, name = name, createdAt = Clock.System.now()))
        return id
    }
    override suspend fun deleteCollection(id: Long) {
        collectionsFlow.tryEmit((collectionsFlow.replayCache.firstOrNull() ?: emptyList()).filter { it.id != id })
    }

    // Bookmarks
    override fun getBookmarksForBook(bookUri: String): Flow<List<Bookmark>> =
        bookmarksFlow.map { it.filter { b -> b.bookUri == bookUri } }
    override fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarksFlow
    override suspend fun addBookmark(bookmark: Bookmark): Long {
        val current = bookmarksFlow.replayCache.firstOrNull() ?: emptyList()
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        bookmarksFlow.tryEmit(current + bookmark.copy(id = id))
        return id
    }
    override suspend fun deleteBookmark(id: Long) {
        bookmarksFlow.tryEmit((bookmarksFlow.replayCache.firstOrNull() ?: emptyList()).filter { it.id != id })
    }

    // Highlights
    override fun getHighlightsForBook(bookUri: String): Flow<List<Highlight>> =
        highlightsFlow.map { it.filter { h -> h.bookUri == bookUri } }
    override fun getHighlightsForPage(bookUri: String, pageIndex: Int): Flow<List<Highlight>> =
        highlightsFlow.map { it.filter { h -> h.bookUri == bookUri && h.pageIndex == pageIndex } }
    override suspend fun addHighlight(highlight: Highlight): Long {
        val current = highlightsFlow.replayCache.firstOrNull() ?: emptyList()
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        highlightsFlow.tryEmit(current + highlight.copy(id = id))
        return id
    }
    override suspend fun deleteHighlight(id: Long) {
        highlightsFlow.tryEmit((highlightsFlow.replayCache.firstOrNull() ?: emptyList()).filter { it.id != id })
    }
    override suspend fun updateHighlightNote(id: Long, note: String) {
        val current = highlightsFlow.replayCache.firstOrNull() ?: return
        highlightsFlow.tryEmit(current.map { if (it.id == id) it.copy(note = note) else it })
    }

    // Tags
    override fun getAllTagNames(): Flow<List<String>> = tagsFlow.map { it.map { t -> t.tagName }.distinct() }
    override fun getTagsForBook(bookUri: String): Flow<List<Tag>> =
        tagsFlow.map { it.filter { t -> t.bookUri == bookUri } }
    override suspend fun addTag(bookUri: String, tagName: String) {
        val current = tagsFlow.replayCache.firstOrNull() ?: emptyList()
        if (current.none { it.bookUri == bookUri && it.tagName == tagName })
            tagsFlow.tryEmit(current + Tag(bookUri = bookUri, tagName = tagName))
    }
    override suspend fun removeTag(bookUri: String, tagName: String) {
        tagsFlow.tryEmit((tagsFlow.replayCache.firstOrNull() ?: emptyList())
            .filter { !(it.bookUri == bookUri && it.tagName == tagName) })
    }

    // Preferences
    override fun getUserPreferences(): Flow<UserPreferences> = preferencesFlow
    private fun updatePrefs(update: UserPreferences.() -> UserPreferences) {
        preferencesFlow.tryEmit((preferencesFlow.replayCache.firstOrNull() ?: UserPreferences()).update())
    }
    override suspend fun setAccentColor(color: AccentColor) = updatePrefs { copy(accentColor = color) }
    override suspend fun setFontScaleFactor(scale: Float) = updatePrefs { copy(fontScaleFactor = scale) }
    override suspend fun setReadingTheme(theme: ReadingTheme) = updatePrefs { copy(readingTheme = theme) }
    override suspend fun setUseDynamicColor(use: Boolean) = updatePrefs { copy(useDynamicColor = use) }
    override suspend fun setLastOpenedBookUri(uri: String?) = updatePrefs { copy(lastOpenedBookUri = uri) }
    override suspend fun setLineSpacing(multiplier: Float) = updatePrefs { copy(lineSpacingMultiplier = multiplier) }
    override suspend fun setHorizontalMargin(dp: Int) = updatePrefs { copy(horizontalMarginDp = dp) }
    override suspend fun setFontFamily(family: FontFamily) = updatePrefs { copy(fontFamily = family) }
    override suspend fun setBrightness(value: Float) = updatePrefs { copy(brightness = value) }
    override suspend fun setBlueLightFilter(enabled: Boolean) = updatePrefs { copy(blueLightFilter = enabled) }
    override suspend fun setCustomBackgroundColor(argb: Long?) = updatePrefs { copy(customBackgroundColor = argb) }
    override suspend fun setPageTurnMode(mode: PageTurnMode) = updatePrefs { copy(pageTurnMode = mode) }
    override suspend fun setAutoScroll(enabled: Boolean) = updatePrefs { copy(autoScrollEnabled = enabled) }
    override suspend fun setAutoScrollSpeed(speed: Float) = updatePrefs { copy(autoScrollSpeed = speed) }
    override suspend fun setPdfCropMargins(enabled: Boolean) = updatePrefs { copy(pdfCropMargins = enabled) }
    override suspend fun setPdfHighContrast(enabled: Boolean) = updatePrefs { copy(pdfHighContrast = enabled) }
    override suspend fun setPdfFillMode(mode: com.jegly.pages.core.model.PdfFillMode) = updatePrefs { copy(pdfFillMode = mode) }
    override suspend fun setShowPageNumber(enabled: Boolean) = updatePrefs { copy(showPageNumber = enabled) }
    override suspend fun setTtsSpeed(speed: Float) = updatePrefs { copy(ttsSpeed = speed) }
    override suspend fun setTtsPitch(pitch: Float) = updatePrefs { copy(ttsPitch = pitch) }
    override suspend fun setEInkMode(enabled: Boolean) = updatePrefs { copy(eInkMode = enabled) }
    override suspend fun setBiometricLockEnabled(enabled: Boolean) = updatePrefs { copy(biometricLockEnabled = enabled) }
    override suspend fun setPdfRequireAuth(enabled: Boolean) = updatePrefs { copy(pdfRequireAuth = enabled) }
    override suspend fun setAutoClearCache(enabled: Boolean) = updatePrefs { copy(autoClearCache = enabled) }
}
