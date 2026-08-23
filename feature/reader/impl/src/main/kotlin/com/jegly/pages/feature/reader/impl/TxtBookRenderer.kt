package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.net.Uri
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import com.jegly.pages.core.rendering.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val CHARS_PER_PAGE = 3000
private const val MAX_READ_BYTES = 10 * 1024 * 1024L // 10 MB hard cap

private fun InputStream.readTextCapped(maxBytes: Long): String {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    var remaining = maxBytes
    while (remaining > 0) {
        val read = read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
        if (read == -1) break
        out.write(buf, 0, read)
        remaining -= read
    }
    return out.toString(Charsets.UTF_8.name())
}

class TxtBookRenderer(
    private val context: Context,
    private val uri: Uri,
) : BookRenderer {

    private var pages: List<String> = emptyList()
    @Volatile private var loaded = false
    private val loadMutex = Mutex()

    private suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readTextCapped(MAX_READ_BYTES)
                } ?: ""
                pages = text.chunked(CHARS_PER_PAGE)
            }
            loaded = true
        }
    }

    override val pageCount: Int get() = if (loaded) maxOf(pages.size, 1) else 1

    override suspend fun getPage(index: Int): PageContent {
        ensureLoaded()
        return if (index in pages.indices) PageContent.TextPage(pages[index], index)
        else PageContent.Error("Page not found")
    }

    override suspend fun searchPages(query: String): List<SearchResult> {
        ensureLoaded()
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
}
