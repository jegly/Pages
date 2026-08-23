package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.net.Uri
import android.text.Html
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import com.jegly.pages.core.rendering.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

private const val MOBI_CHARS_PER_PAGE = 3000
private const val MOBI_MAX_FILE_BYTES = 50L * 1024 * 1024  // 50 MB

private const val COMPRESSION_NONE = 1
private const val COMPRESSION_PALMDOC = 2
// 17480 = HUFF/CDIC — not supported

class MobiBookRenderer(
    private val context: Context,
    private val uri: Uri,
) : BookRenderer {

    private var pages: List<String> = emptyList()
    @Volatile private var initialized = false
    private val initMutex = Mutex()

    override val pageCount: Int get() = if (initialized) maxOf(pages.size, 1) else 1

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initMutex.withLock {
            if (initialized) return@withLock
            runCatching {
                val text = parseMobi() ?: ""
                pages = if (text.isBlank()) emptyList() else text.chunked(MOBI_CHARS_PER_PAGE)
            }
            initialized = true
        }
    }

    override suspend fun getPage(index: Int): PageContent {
        ensureInitialized()
        if (pages.isEmpty()) return PageContent.Error("Could not read MOBI file")
        if (index !in pages.indices) return PageContent.Error("Page not found")
        return PageContent.TextPage(pages[index], index)
    }

    override suspend fun searchPages(query: String): List<SearchResult> {
        ensureInitialized()
        if (query.isBlank()) return emptyList()
        return pages.mapIndexedNotNull { index, page ->
            val matchStart = page.indexOf(query, ignoreCase = true)
            if (matchStart < 0) return@mapIndexedNotNull null
            val snippetStart = (matchStart - 30).coerceAtLeast(0)
            val snippetEnd = (matchStart + 30).coerceAtMost(page.length)
            SearchResult(pageIndex = index, snippet = page.substring(snippetStart, snippetEnd))
        }
    }

    override fun close() = Unit

    private fun parseMobi(): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(65_536)
            var total = 0L
            var n: Int
            while (stream.read(buf).also { n = it } >= 0) {
                total += n
                if (total > MOBI_MAX_FILE_BYTES) return null
                out.write(buf, 0, n)
            }
            out.toByteArray()
        } ?: return null

        if (bytes.size < 78) return null
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // Validate PalmDB type/creator ("BOOK"/"MOBI" or "BOOK"/"REAd")
        val type = String(bytes, 60, 4, Charsets.US_ASCII)
        val creator = String(bytes, 64, 4, Charsets.US_ASCII)
        if (type != "BOOK" || (creator != "MOBI" && creator != "REAd")) return null

        val numRecords = bb.getShort(76).toInt() and 0xFFFF
        if (numRecords < 2) return null

        // Record offsets from the record list starting at byte 78 (8 bytes each).
        val recordOffsets = IntArray(numRecords) { i -> bb.getInt(78 + i * 8) }

        val rec0 = recordOffsets[0]
        if (rec0 + 16 > bytes.size) return null

        val compression = bb.getShort(rec0 + 0).toInt() and 0xFFFF
        val textRecordCount = bb.getShort(rec0 + 8).toInt() and 0xFFFF

        if (compression != COMPRESSION_NONE && compression != COMPRESSION_PALMDOC) {
            return null  // HUFF/CDIC not supported
        }

        // Detect text encoding from MOBI header (starts at rec0 + 16).
        val mobiMagicOffset = rec0 + 16
        val charset = if (mobiMagicOffset + 16 <= bytes.size &&
            bytes[mobiMagicOffset + 0] == 'M'.code.toByte() &&
            bytes[mobiMagicOffset + 1] == 'O'.code.toByte() &&
            bytes[mobiMagicOffset + 2] == 'B'.code.toByte() &&
            bytes[mobiMagicOffset + 3] == 'I'.code.toByte()
        ) {
            when (bb.getInt(mobiMagicOffset + 12)) {
                1252 -> runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)
                else -> Charsets.UTF_8
            }
        } else {
            Charsets.UTF_8
        }

        // Decompress text records 1..textRecordCount.
        val rawHtml = ByteArrayOutputStream()
        for (i in 1..textRecordCount) {
            if (i >= numRecords) break
            val start = recordOffsets[i]
            val end = if (i + 1 < numRecords) recordOffsets[i + 1] else bytes.size
            if (start >= bytes.size || start >= end) break
            val record = bytes.copyOfRange(start, end.coerceAtMost(bytes.size))
            when (compression) {
                COMPRESSION_NONE -> rawHtml.write(record)
                COMPRESSION_PALMDOC -> rawHtml.write(decompressPalmDoc(record))
            }
        }

        // Replace MOBI page-break tags with blank lines so paragraph flow is preserved.
        val html = rawHtml.toString(charset.name())
            .replace(Regex("<mbp:pagebreak[^>]*/?>", RegexOption.IGNORE_CASE), "\n\n")

        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    companion object {
        // PalmDOC LZ77 decompression. Each record is at most 4096 bytes uncompressed.
        fun decompressPalmDoc(data: ByteArray): ByteArray {
            val out = ByteArray(8192)
            var size = 0
            var i = 0

            fun put(v: Int) { if (size < out.size) out[size++] = v.toByte() }

            while (i < data.size) {
                val b = data[i++].toInt() and 0xFF
                when {
                    b == 0x00 -> put(b)
                    b in 0x01..0x08 -> repeat(b) { if (i < data.size) put(data[i++].toInt() and 0xFF) }
                    b in 0x09..0x7F -> put(b)
                    b in 0x80..0xBF -> {
                        if (i >= data.size) return out.copyOf(size)
                        val b2 = data[i++].toInt() and 0xFF
                        val w = ((b and 0x3F) shl 8) or b2
                        val distance = w ushr 3
                        val length = (w and 0x07) + 3
                        val startPos = size - distance
                        if (startPos >= 0 && distance > 0) {
                            repeat(length) { j ->
                                if (size < out.size) put(out[startPos + j % distance].toInt() and 0xFF)
                            }
                        }
                    }
                    else -> { put(0x20); put(b xor 0x80) }
                }
            }
            return out.copyOf(size)
        }
    }
}
