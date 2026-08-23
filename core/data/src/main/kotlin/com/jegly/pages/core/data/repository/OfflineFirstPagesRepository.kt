package com.jegly.pages.core.data.repository

import com.jegly.pages.core.database.dao.BookDao
import com.jegly.pages.core.database.dao.BookmarkDao
import com.jegly.pages.core.database.dao.CollectionDao
import com.jegly.pages.core.database.dao.HighlightDao
import com.jegly.pages.core.database.dao.TagDao
import com.jegly.pages.core.database.model.CollectionEntity
import com.jegly.pages.core.database.model.TagEntity
import com.jegly.pages.core.database.model.asEntity
import com.jegly.pages.core.database.model.asExternalModel
import com.jegly.pages.core.datastore.PagesPreferencesDataSource
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.Bookmark
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.Collection
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.Highlight
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.PdfFillMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.SliderTimeout
import com.jegly.pages.core.model.Tag
import com.jegly.pages.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

class OfflineFirstPagesRepository @Inject constructor(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val highlightDao: HighlightDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao,
    private val preferencesDataSource: PagesPreferencesDataSource,
) : PagesRepository {

    override fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { it.map { e -> e.asExternalModel() } }

    override fun getBook(uri: String): Flow<Book?> =
        bookDao.getBook(uri).map { it?.asExternalModel() }

    override fun getRecentBooks(limit: Int): Flow<List<Book>> =
        bookDao.getRecentBooks(limit).map { it.map { e -> e.asExternalModel() } }

    override fun searchBooks(query: String): Flow<List<Book>> {
        // Wrap in double-quotes so the full phrase is treated as a literal FTS token,
        // then append * for prefix matching. Interior quotes are stripped to prevent
        // malformed FTS expressions that would cause an SQLiteException.
        val safe = "\"${query.replace("\"", "")}*\""
        return bookDao.searchBooks(safe).map { it.map { e -> e.asExternalModel() } }
    }

    override fun getBooksByTag(tagName: String): Flow<List<Book>> =
        tagDao.getBooksByTag(tagName).map { it.map { e -> e.asExternalModel() } }

    override fun getBooksByCollection(collectionId: Long): Flow<List<Book>> =
        bookDao.getBooksByCollection(collectionId).map { it.map { e -> e.asExternalModel() } }

    override suspend fun upsertBook(book: Book) = bookDao.insertBook(book.asEntity())

    override suspend fun upsertBooks(books: List<Book>) =
        bookDao.insertBooks(books.map { it.asEntity() })

    override suspend fun updateProgress(uri: String, position: String, percent: Float) =
        bookDao.updateProgress(uri, position, percent, Clock.System.now().toEpochMilliseconds())

    override suspend fun moveBookToCollection(bookUri: String, collectionId: Long?) =
        bookDao.moveBookToCollection(bookUri, collectionId)

    override suspend fun deleteBook(uri: String) = bookDao.deleteBook(uri)

    override fun getAllCollections(): Flow<List<Collection>> =
        collectionDao.getAllCollections().map { it.map { e -> e.asExternalModel() } }

    override suspend fun createCollection(name: String): Long =
        collectionDao.insertCollection(
            CollectionEntity(name = name, createdAt = Clock.System.now().toEpochMilliseconds()),
        )

    override suspend fun deleteCollection(id: Long) = collectionDao.deleteCollection(id)

    override fun getBookmarksForBook(bookUri: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForBook(bookUri).map { it.map { e -> e.asExternalModel() } }

    override fun getAllBookmarks(): Flow<List<Bookmark>> =
        bookmarkDao.getAllBookmarks().map { it.map { e -> e.asExternalModel() } }

    override suspend fun addBookmark(bookmark: Bookmark): Long =
        bookmarkDao.insertBookmark(bookmark.asEntity())

    override suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteBookmark(id)

    override fun getHighlightsForBook(bookUri: String): Flow<List<Highlight>> =
        highlightDao.getHighlightsForBook(bookUri).map { it.map { e -> e.asExternalModel() } }

    override fun getHighlightsForPage(bookUri: String, pageIndex: Int): Flow<List<Highlight>> =
        highlightDao.getHighlightsForPage(bookUri, pageIndex).map { it.map { e -> e.asExternalModel() } }

    override suspend fun addHighlight(highlight: Highlight): Long =
        highlightDao.insertHighlight(highlight.asEntity())

    override suspend fun deleteHighlight(id: Long) = highlightDao.deleteHighlight(id)

    override suspend fun updateHighlightNote(id: Long, note: String) =
        highlightDao.updateNote(id, note)

    override fun getAllTagNames(): Flow<List<String>> = tagDao.getAllTagNames()

    override fun getTagsForBook(bookUri: String): Flow<List<Tag>> =
        tagDao.getTagsForBook(bookUri).map { it.map { e -> e.asExternalModel() } }

    override suspend fun addTag(bookUri: String, tagName: String) =
        tagDao.insertTag(TagEntity(bookUri = bookUri, tagName = tagName))

    override suspend fun removeTag(bookUri: String, tagName: String) =
        tagDao.deleteTag(bookUri, tagName)

    override fun getUserPreferences(): Flow<UserPreferences> =
        preferencesDataSource.userPreferences

    override suspend fun setAccentColor(color: AccentColor) {
        preferencesDataSource.setAccentColor(color)
    }

    override suspend fun setFontScaleFactor(scale: Float) {
        preferencesDataSource.setFontScaleFactor(scale)
    }

    override suspend fun setReadingTheme(theme: ReadingTheme) {
        preferencesDataSource.setReadingTheme(theme)
    }

    override suspend fun setUseDynamicColor(use: Boolean) {
        preferencesDataSource.setUseDynamicColor(use)
    }

    override suspend fun setLastOpenedBookUri(uri: String?) {
        preferencesDataSource.setLastOpenedBookUri(uri)
    }

    override suspend fun setLineSpacing(multiplier: Float) {
        preferencesDataSource.setLineSpacing(multiplier)
    }

    override suspend fun setHorizontalMargin(dp: Int) {
        preferencesDataSource.setHorizontalMargin(dp)
    }

    override suspend fun setFontFamily(family: FontFamily) {
        preferencesDataSource.setFontFamily(family)
    }

    override suspend fun setBrightness(value: Float) {
        preferencesDataSource.setBrightness(value)
    }

    override suspend fun setBlueLightFilter(enabled: Boolean) {
        preferencesDataSource.setBlueLightFilter(enabled)
    }

    override suspend fun setCustomBackgroundColor(argb: Long?) {
        preferencesDataSource.setCustomBackgroundColor(argb)
    }

    override suspend fun setPageTurnMode(mode: PageTurnMode) {
        preferencesDataSource.setPageTurnMode(mode)
    }

    override suspend fun setAutoScroll(enabled: Boolean) {
        preferencesDataSource.setAutoScroll(enabled)
    }

    override suspend fun setAutoScrollSpeed(speed: Float) {
        preferencesDataSource.setAutoScrollSpeed(speed)
    }

    override suspend fun setPdfCropMargins(enabled: Boolean) {
        preferencesDataSource.setPdfCropMargins(enabled)
    }

    override suspend fun setPdfHighContrast(enabled: Boolean) {
        preferencesDataSource.setPdfHighContrast(enabled)
    }

    override suspend fun setPdfFillMode(mode: PdfFillMode) {
        preferencesDataSource.setPdfFillMode(mode)
    }

    override suspend fun setShowPageNumber(enabled: Boolean) {
        preferencesDataSource.setShowPageNumber(enabled)
    }

    override suspend fun setShowNavigationButtons(enabled: Boolean) {
        preferencesDataSource.setShowNavigationButtons(enabled)
    }

    override suspend fun setTtsSpeed(speed: Float) {
        preferencesDataSource.setTtsSpeed(speed)
    }

    override suspend fun setTtsPitch(pitch: Float) {
        preferencesDataSource.setTtsPitch(pitch)
    }

    override fun setTtsEngine(engine: String) {
        preferencesDataSource.setTtsEngine(engine)
    }

    override fun setTtsVoiceId(voiceId: String) {
        preferencesDataSource.setTtsVoiceId(voiceId)
    }

    override suspend fun setEInkMode(enabled: Boolean) {
        preferencesDataSource.setEInkMode(enabled)
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        preferencesDataSource.setBiometricLockEnabled(enabled)
    }

    override suspend fun setPdfRequireAuth(enabled: Boolean) {
        preferencesDataSource.setPdfRequireAuth(enabled)
    }

    override suspend fun setAllowScreenshots(enabled: Boolean) {
        preferencesDataSource.setAllowScreenshots(enabled)
    }

    override suspend fun setAutoClearCache(enabled: Boolean) {
        preferencesDataSource.setAutoClearCache(enabled)
    }

    override suspend fun setSliderTimeout(timeout: SliderTimeout) {
        preferencesDataSource.setSliderTimeout(timeout)
    }
}
