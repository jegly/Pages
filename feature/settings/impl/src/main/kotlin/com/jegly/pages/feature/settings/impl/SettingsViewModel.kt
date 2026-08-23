package com.jegly.pages.feature.settings.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jegly.pages.core.data.extractCover
import com.jegly.pages.core.data.repository.PagesRepository
import com.jegly.pages.core.security.EncryptedFileManager
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.SliderTimeout
import com.jegly.pages.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val done: Int = 0, val total: Int = 0) : ScanState
    data class Done(val count: Int) : ScanState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PagesRepository,
    @ApplicationContext private val context: Context,
    private val encryptedFileManager: EncryptedFileManager,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.getUserPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(),
        )

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun setAccentColor(color: AccentColor) = viewModelScope.launch { repository.setAccentColor(color) }
    fun setFontScale(scale: Float) = viewModelScope.launch { repository.setFontScaleFactor(scale) }
    fun setReadingTheme(theme: ReadingTheme) = viewModelScope.launch { repository.setReadingTheme(theme) }
    fun setUseDynamicColor(use: Boolean) = viewModelScope.launch { repository.setUseDynamicColor(use) }
    fun setFontFamily(family: FontFamily) = viewModelScope.launch { repository.setFontFamily(family) }
    fun setLineSpacing(multiplier: Float) = viewModelScope.launch { repository.setLineSpacing(multiplier) }
    fun setHorizontalMargin(dp: Int) = viewModelScope.launch { repository.setHorizontalMargin(dp) }
    fun setPageTurnMode(mode: PageTurnMode) = viewModelScope.launch { repository.setPageTurnMode(mode) }
    fun setBlueLightFilter(enabled: Boolean) = viewModelScope.launch { repository.setBlueLightFilter(enabled) }
    fun setAutoScrollSpeed(speed: Float) = viewModelScope.launch { repository.setAutoScrollSpeed(speed) }
    fun setPdfHighContrast(enabled: Boolean) = viewModelScope.launch { repository.setPdfHighContrast(enabled) }
    fun setShowPageNumber(enabled: Boolean) = viewModelScope.launch { repository.setShowPageNumber(enabled) }
    fun setShowNavigationButtons(enabled: Boolean) = viewModelScope.launch { repository.setShowNavigationButtons(enabled) }
    fun setPdfCropMargins(enabled: Boolean) = viewModelScope.launch { repository.setPdfCropMargins(enabled) }
    fun setEInkMode(enabled: Boolean) = viewModelScope.launch { repository.setEInkMode(enabled) }
    fun setBiometricLockEnabled(enabled: Boolean) = viewModelScope.launch { repository.setBiometricLockEnabled(enabled) }
    fun setPdfRequireAuth(enabled: Boolean) = viewModelScope.launch { repository.setPdfRequireAuth(enabled) }
    fun setAllowScreenshots(enabled: Boolean) = viewModelScope.launch { repository.setAllowScreenshots(enabled) }
    fun setAutoClearCache(enabled: Boolean) = viewModelScope.launch { repository.setAutoClearCache(enabled) }
    fun setSliderTimeout(timeout: SliderTimeout) = viewModelScope.launch { repository.setSliderTimeout(timeout) }

    fun clearCache() = viewModelScope.launch(Dispatchers.IO) {
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }

    fun dismissScanResult() {
        _scanState.value = ScanState.Idle
    }

    fun scanAndImport(treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _scanState.value = ScanState.Scanning()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val total = countRecursive(treeUri, rootDocId)
                _scanState.value = ScanState.Scanning(done = 0, total = total)
                val doneCounter = AtomicInteger(0)
                val imported = importRecursive(treeUri, rootDocId, total, doneCounter)
                _scanState.value = ScanState.Done(imported)
            } catch (e: Exception) {
                val done = (_scanState.value as? ScanState.Scanning)?.done ?: 0
                _scanState.value = ScanState.Done(done)
            }
        }
    }

    private fun countRecursive(treeUri: Uri, parentDocId: String): Int {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        var count = 0
        runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        count += countRecursive(treeUri, docId)
                    } else if (formatFor(name) != null) {
                        count++
                    }
                }
            }
        }
        return count
    }

    private suspend fun importRecursive(
        treeUri: Uri,
        parentDocId: String,
        total: Int,
        doneCounter: AtomicInteger,
    ): Int {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        var count = 0
        runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null, null, null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        count += importRecursive(treeUri, docId, total, doneCounter)
                    } else {
                        val format = formatFor(name) ?: continue
                        // Each file is imported independently — one failure never aborts the scan.
                        runCatching {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val coverUri = extractCover(context, docUri, format)
                            val shouldEncrypt = preferences.value.biometricLockEnabled
                            val (bookUri, isEncrypted) = if (shouldEncrypt) {
                                val encFile = encryptedFileManager.encryptFromUri(docUri)
                                if (encFile != null) Pair(encFile.absolutePath, true)
                                else Pair(docUri.toString(), false)
                            } else {
                                Pair(docUri.toString(), false)
                            }
                            repository.upsertBook(
                                Book(
                                    uri = bookUri,
                                    title = name.substringBeforeLast('.'),
                                    authors = "",
                                    coverUri = coverUri,
                                    format = format,
                                    fileSize = size,
                                    dateAdded = Clock.System.now(),
                                    lastOpened = null,
                                    currentPosition = "0",
                                    progressPercent = 0f,
                                    collectionId = null,
                                    isEncrypted = isEncrypted,
                                ),
                            )
                            count++
                        }
                        val done = doneCounter.incrementAndGet()
                        _scanState.value = ScanState.Scanning(done, total)
                    }
                }
            }
        }
        return count
    }

    private fun formatFor(name: String): BookFormat? = when {
        name.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
        name.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
        name.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
        name.endsWith(".mobi", ignoreCase = true) -> BookFormat.MOBI
        name.endsWith(".cbz", ignoreCase = true) -> BookFormat.CBZ
        name.endsWith(".cbr", ignoreCase = true) -> BookFormat.CBR
        else -> null
    }
}
