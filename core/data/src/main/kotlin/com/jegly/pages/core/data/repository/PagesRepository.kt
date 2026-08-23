package com.jegly.pages.core.data.repository

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

interface PagesRepository {

    // Books
    fun getAllBooks(): Flow<List<Book>>
    fun getBook(uri: String): Flow<Book?>
    fun getRecentBooks(limit: Int = 10): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    fun getBooksByTag(tagName: String): Flow<List<Book>>
    fun getBooksByCollection(collectionId: Long): Flow<List<Book>>
    suspend fun upsertBook(book: Book)
    suspend fun upsertBooks(books: List<Book>)
    suspend fun updateProgress(uri: String, position: String, percent: Float)
    suspend fun moveBookToCollection(bookUri: String, collectionId: Long?)
    suspend fun deleteBook(uri: String)

    // Collections
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun createCollection(name: String): Long
    suspend fun deleteCollection(id: Long)

    // Bookmarks
    fun getBookmarksForBook(bookUri: String): Flow<List<Bookmark>>
    fun getAllBookmarks(): Flow<List<Bookmark>>
    suspend fun addBookmark(bookmark: Bookmark): Long
    suspend fun deleteBookmark(id: Long)

    // Highlights
    fun getHighlightsForBook(bookUri: String): Flow<List<Highlight>>
    fun getHighlightsForPage(bookUri: String, pageIndex: Int): Flow<List<Highlight>>
    suspend fun addHighlight(highlight: Highlight): Long
    suspend fun deleteHighlight(id: Long)
    suspend fun updateHighlightNote(id: Long, note: String)

    // Tags
    fun getAllTagNames(): Flow<List<String>>
    fun getTagsForBook(bookUri: String): Flow<List<Tag>>
    suspend fun addTag(bookUri: String, tagName: String)
    suspend fun removeTag(bookUri: String, tagName: String)

    // Preferences
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setFontScaleFactor(scale: Float)
    suspend fun setReadingTheme(theme: ReadingTheme)
    suspend fun setUseDynamicColor(use: Boolean)
    suspend fun setLastOpenedBookUri(uri: String?)
    suspend fun setLineSpacing(multiplier: Float)
    suspend fun setHorizontalMargin(dp: Int)
    suspend fun setFontFamily(family: FontFamily)
    suspend fun setBrightness(value: Float)
    suspend fun setBlueLightFilter(enabled: Boolean)
    suspend fun setCustomBackgroundColor(argb: Long?)
    suspend fun setPageTurnMode(mode: PageTurnMode)
    suspend fun setAutoScroll(enabled: Boolean)
    suspend fun setAutoScrollSpeed(speed: Float)
    suspend fun setPdfCropMargins(enabled: Boolean)
    suspend fun setPdfHighContrast(enabled: Boolean)
    suspend fun setPdfFillMode(mode: PdfFillMode)
    suspend fun setShowPageNumber(enabled: Boolean)
    suspend fun setShowNavigationButtons(enabled: Boolean)
    suspend fun setTtsSpeed(speed: Float)
    suspend fun setTtsPitch(pitch: Float)
    fun setTtsEngine(engine: String)
    fun setTtsVoiceId(voiceId: String)
    suspend fun setEInkMode(enabled: Boolean)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setPdfRequireAuth(enabled: Boolean)
    suspend fun setAllowScreenshots(enabled: Boolean)
    suspend fun setAutoClearCache(enabled: Boolean)
    suspend fun setSliderTimeout(timeout: SliderTimeout)
}
