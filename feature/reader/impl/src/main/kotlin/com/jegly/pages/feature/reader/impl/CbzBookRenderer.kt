package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private const val CBZ_MAX_TOTAL_BYTES = 200L * 1024 * 1024  // 200 MB across all pages
private const val CBZ_MAX_ENTRY_BYTES = 30L * 1024 * 1024   // 30 MB per image

class CbzBookRenderer(
    private val context: Context,
    private val uri: Uri,
) : BookRenderer {

    private var imageData: List<ByteArray> = emptyList()
    @Volatile private var initialized = false
    private val initMutex = Mutex()

    override val pageCount: Int get() = if (initialized) maxOf(imageData.size, 1) else 1

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initMutex.withLock {
            if (initialized) return@withLock
            runCatching { imageData = loadImages() }
            initialized = true
        }
    }

    private fun loadImages(): List<ByteArray> {
        // TreeMap sorts entries alphabetically, which is reading order for all known CBZ tools.
        val entries = sortedMapOf<String, ByteArray>(compareBy { it.lowercase() })
        var totalBytes = 0L
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val ext = entry.name.substringAfterLast('.').lowercase()
                        if (ext in IMAGE_EXTENSIONS) {
                            val data = readEntryLimited(zip) ?: break
                            totalBytes += data.size
                            if (totalBytes > CBZ_MAX_TOTAL_BYTES) break
                            entries[entry.name] = data
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return entries.values.toList()
    }

    override suspend fun getPage(index: Int): PageContent = withContext(Dispatchers.IO) {
        ensureInitialized()
        if (imageData.isEmpty()) return@withContext PageContent.Error("No images found in CBZ")
        val data = imageData.getOrNull(index) ?: return@withContext PageContent.Error("Page not found")
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: return@withContext PageContent.Error("Could not decode image")
        PageContent.BitmapPage(bitmap, index)
    }

    override fun close() = Unit

    companion object {
        fun readFirstImage(context: Context, uri: Uri): ByteArray? {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    val candidates = mutableMapOf<String, ByteArray>()
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val ext = entry.name.substringAfterLast('.').lowercase()
                            if (ext in IMAGE_EXTENSIONS) {
                                val data = readEntryLimited(zip)
                                if (data != null) candidates[entry.name] = data
                                if (candidates.size >= 3) break
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    return candidates.entries.minByOrNull { it.key.lowercase() }?.value
                }
            }
            return null
        }

        private fun readEntryLimited(zip: ZipInputStream): ByteArray? {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(65_536)
            var entryBytes = 0L
            var n: Int
            while (zip.read(buf).also { n = it } >= 0) {
                entryBytes += n
                if (entryBytes > CBZ_MAX_ENTRY_BYTES) return null
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }
}
