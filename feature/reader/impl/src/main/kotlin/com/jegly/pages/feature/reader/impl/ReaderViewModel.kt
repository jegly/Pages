package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.security.EncryptedFileManager
import com.jegly.pages.core.model.Bookmark
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.Highlight
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.PdfFillMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.UserPreferences
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import com.jegly.pages.core.rendering.SearchResult
import com.jegly.pages.core.rendering.TocEntry
import com.jegly.pages.core.tts.TtsManager
import com.jegly.pages.core.tts.TtsState
import kotlinx.coroutines.flow.SharedFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject

data class ReaderUiState(
    val title: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val content: PageContent = PageContent.Loading,
    val nextContent: PageContent? = null,
    val isBookmarked: Boolean = false,
    val highlights: List<Highlight> = emptyList(),
    val bookFormat: BookFormat? = null,
)

@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    @Assisted val bookUri: String,
    @Assisted val startPage: Int,
    @ApplicationContext private val context: Context,
    private val repository: PagesRepository,
    private val encryptedFileManager: EncryptedFileManager,
    private val ttsManager: TtsManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(bookUri: String, startPage: Int): ReaderViewModel
    }

    private var renderer: BookRenderer? = null
    private var loadJob: Job? = null
    private var tempDecryptedFile: File? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _tableOfContents = MutableStateFlow<List<TocEntry>>(emptyList())
    val tableOfContents: StateFlow<List<TocEntry>> = _tableOfContents.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    val preferences: StateFlow<UserPreferences> = repository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val ttsState: StateFlow<TtsState> = ttsManager.state
    val ttsPageCompleted: SharedFlow<Unit> = ttsManager.pageCompleted

    init {
        loadBook()
        observeBookmark()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = repository.getBook(bookUri).first()

            if (book == null) {
                _uiState.value = _uiState.value.copy(content = PageContent.Error("Book not found"))
                return@launch
            }

            // For encrypted books: decrypt to a temp file in cache, then point the renderer
            // at that file. The temp file is cleaned up when the ViewModel is cleared.
            val uri = if (book.isEncrypted) {
                val encFile = File(book.uri)
                val temp = encryptedFileManager.decryptToTempFile(encFile)
                tempDecryptedFile = temp
                Uri.fromFile(temp)
            } else {
                Uri.parse(book.uri)
            }

            renderer = when (book.format) {
                BookFormat.PDF -> PdfBookRenderer(context, uri)
                BookFormat.TXT -> TxtBookRenderer(context, uri)
                BookFormat.EPUB -> EpubBookRenderer(context, uri)
                BookFormat.MOBI -> MobiBookRenderer(context, uri)
                BookFormat.CBZ -> CbzBookRenderer(context, uri)
                else -> null
            }

            val page = if (startPage >= 0) startPage
                       else book.currentPosition.toIntOrNull() ?: 0
            _uiState.value = _uiState.value.copy(
                title = book.title,
                currentPage = page,
                totalPages = renderer?.pageCount ?: 0,
                bookFormat = book.format,
            )
            loadPage(page)
            _tableOfContents.value = renderer?.tableOfContents ?: emptyList()
            repository.setLastOpenedBookUri(bookUri)
        }
    }

    private fun observeBookmark() {
        viewModelScope.launch {
            repository.getBookmarksForBook(bookUri).collect { bookmarks ->
                val currentPage = _uiState.value.currentPage
                _uiState.value = _uiState.value.copy(
                    isBookmarked = bookmarks.any { it.position == currentPage.toString() },
                )
            }
        }
    }

    fun goToPage(page: Int) {
        val total = _uiState.value.totalPages
        if (total == 0 || page < 0 || page >= total) return
        if (ttsManager.state.value.isActive) ttsManager.stop()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadPage(page)
            val percent = if (total > 0) page.toFloat() / total else 0f
            repository.updateProgress(bookUri, page.toString(), percent)
        }
    }

    private suspend fun loadPage(page: Int) {
        val oldContent = _uiState.value.content
        val oldNextContent = _uiState.value.nextContent
        // Load new content without going through a Loading state — keeps old content visible
        // on screen while the new bitmaps are rendered, eliminating the flash on page turn.
        val content = maybeApplyCropMargins(renderer?.getPage(page) ?: PageContent.Error("Renderer unavailable"))
        val total = renderer?.pageCount ?: 0
        val nextContent = if (page + 1 < total) renderer?.getPage(page + 1)?.let { maybeApplyCropMargins(it) } else null
        val bookmarks = repository.getBookmarksForBook(bookUri).first()
        val highlights = repository.getHighlightsForPage(bookUri, page).first()
        _uiState.value = _uiState.value.copy(
            currentPage = page,
            totalPages = total,
            content = content,
            nextContent = nextContent,
            isBookmarked = bookmarks.any { it.position == page.toString() },
            highlights = highlights,
        )
        // Recycle old bitmaps in a non-cancellable block so rapid page turns don't leak them.
        // The delay gives Compose time to render with the new bitmaps before we free the old ones.
        withContext(NonCancellable) {
            delay(200)
            (oldContent as? PageContent.BitmapPage)?.bitmap?.recycle()
            (oldNextContent as? PageContent.BitmapPage)?.bitmap?.recycle()
        }
    }

    private suspend fun maybeApplyCropMargins(content: PageContent): PageContent {
        if (!preferences.value.pdfCropMargins || content !is PageContent.BitmapPage) return content
        return withContext(Dispatchers.Default) {
            val original = content.bitmap
            val cropped = cropWhiteMargins(original)
            if (cropped !== original) {
                original.recycle()
                content.copy(bitmap = cropped)
            } else content
        }
    }

    private fun cropWhiteMargins(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w == 0 || h == 0) return bitmap

        // Analyse a scaled-down copy so pixel scanning is cheap regardless of render resolution.
        val scaleW = minOf(w, 512)
        val scaleH = (scaleW.toLong() * h / w).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, scaleW, scaleH, false)
        val pixels = IntArray(scaleW * scaleH)
        small.getPixels(pixels, 0, scaleW, 0, 0, scaleW, scaleH)
        small.recycle()

        var top = scaleH; var bottom = -1; var left = scaleW; var right = -1
        for (y in 0 until scaleH) {
            for (x in 0 until scaleW) {
                val p = pixels[y * scaleW + x]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                // Treat near-white pixels (≥245 on all channels) as blank margin.
                if (r < 245 || g < 245 || b < 245) {
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
        }

        if (bottom < 0 || right < 0) return bitmap // blank page — nothing to crop

        // Map content bounds back to original resolution and add a small padding cushion.
        val origLeft   = (left.toLong()      * w / scaleW).toInt()
        val origTop    = (top.toLong()       * h / scaleH).toInt()
        val origRight  = ((right + 1).toLong() * w / scaleW).toInt().coerceAtMost(w)
        val origBottom = ((bottom + 1).toLong() * h / scaleH).toInt().coerceAtMost(h)

        val padX = (w * 0.01).toInt().coerceAtLeast(4)
        val padY = (h * 0.01).toInt().coerceAtLeast(4)
        val cropLeft = (origLeft - padX).coerceAtLeast(0)
        val cropTop  = (origTop  - padY).coerceAtLeast(0)
        val cropW    = (origRight  - cropLeft + padX).coerceAtMost(w - cropLeft)
        val cropH    = (origBottom - cropTop  + padY).coerceAtMost(h - cropTop)

        if (cropW <= 0 || cropH <= 0) return bitmap
        if (cropLeft == 0 && cropTop == 0 && cropW == w && cropH == h) return bitmap

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropW, cropH)
    }

    fun nextPage() = goToPage(_uiState.value.currentPage + 1)
    fun prevPage() = goToPage(_uiState.value.currentPage - 1)

    fun setRenderWidth(px: Int) {
        (renderer as? PdfBookRenderer)?.let { r ->
            if (px > 0 && px != r.targetWidth) {
                r.targetWidth = px
                reloadCurrentPage()
            }
        }
    }

    fun reloadCurrentPage() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadPage(_uiState.value.currentPage) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val bookmarks = repository.getBookmarksForBook(bookUri).first()
            val existing = bookmarks.find { it.position == page.toString() }
            if (existing != null) {
                repository.deleteBookmark(existing.id)
            } else {
                repository.addBookmark(
                    Bookmark(
                        id = 0,
                        bookUri = bookUri,
                        position = page.toString(),
                        note = "",
                        timestamp = Clock.System.now(),
                    ),
                )
            }
        }
    }

    fun addHighlight(startOffset: Int, endOffset: Int, selectedText: String, colorArgb: Long) {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            repository.addHighlight(
                Highlight(
                    id = 0,
                    bookUri = bookUri,
                    pageIndex = page,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    selectedText = selectedText,
                    colorArgb = colorArgb,
                    note = "",
                    timestamp = Clock.System.now(),
                ),
            )
            // Refresh highlights for current page
            val highlights = repository.getHighlightsForPage(bookUri, page).first()
            _uiState.value = _uiState.value.copy(highlights = highlights)
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
            val page = _uiState.value.currentPage
            val highlights = repository.getHighlightsForPage(bookUri, page).first()
            _uiState.value = _uiState.value.copy(highlights = highlights)
        }
    }

    fun searchInBook(query: String) {
        viewModelScope.launch {
            _searchResults.value = renderer?.searchPages(query) ?: emptyList()
        }
    }

    fun togglePageTurnMode() = viewModelScope.launch {
        val current = preferences.value.pageTurnMode
        repository.setPageTurnMode(if (current == PageTurnMode.Swipe) PageTurnMode.Scroll else PageTurnMode.Swipe)
    }

    fun setPageTurnMode(mode: PageTurnMode) = viewModelScope.launch {
        repository.setPageTurnMode(mode)
    }

    fun toggleAutoScroll() = viewModelScope.launch {
        repository.setAutoScroll(!preferences.value.autoScrollEnabled)
    }

    fun setFontScale(scale: Float) = viewModelScope.launch { repository.setFontScaleFactor(scale) }
    fun setLineSpacing(multiplier: Float) = viewModelScope.launch { repository.setLineSpacing(multiplier) }
    fun setHorizontalMargin(dp: Int) = viewModelScope.launch { repository.setHorizontalMargin(dp) }
    fun setFontFamily(family: FontFamily) = viewModelScope.launch { repository.setFontFamily(family) }
    fun setReadingTheme(theme: ReadingTheme) = viewModelScope.launch { repository.setReadingTheme(theme) }
    fun setPdfFillMode(mode: PdfFillMode) = viewModelScope.launch { repository.setPdfFillMode(mode) }
    fun setBrightness(value: Float) = viewModelScope.launch { repository.setBrightness(value) }
    fun setBlueLightFilter(enabled: Boolean) = viewModelScope.launch { repository.setBlueLightFilter(enabled) }
    fun setTtsSpeed(speed: Float) = viewModelScope.launch { repository.setTtsSpeed(speed) }
    fun setTtsPitch(pitch: Float) = viewModelScope.launch { repository.setTtsPitch(pitch) }

    fun speakCurrentPage() {
        val content = _uiState.value.content
        val text = (content as? PageContent.TextPage)?.text ?: return
        ttsManager.speak(text)
    }

    fun pauseTts() = ttsManager.pause()

    fun resumeTts() = ttsManager.resume()

    fun stopTts() = ttsManager.stop()

    fun setTtsEngine(engine: String) = ttsManager.setEngine(engine)

    fun setTtsVoice(voiceId: String) = ttsManager.setVoice(voiceId)

    fun getTtsAvailableVoices() = ttsManager.availableVoices

    fun clearTtsError() = ttsManager.clearError()

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
        renderer?.close()
        tempDecryptedFile?.delete()
        tempDecryptedFile = null
    }
}
