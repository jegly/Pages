package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.net.Uri
import android.text.Html
import android.util.Xml
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import com.jegly.pages.core.rendering.SearchResult
import com.jegly.pages.core.rendering.TocEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val MAX_ENTRY_BYTES = 50L * 1024 * 1024   // 50 MB per entry
private const val MAX_TOTAL_BYTES = 300L * 1024 * 1024  // 300 MB across whole archive
private const val MAX_ENTRY_COUNT = 2_000

class EpubBookRenderer(
    private val context: Context,
    private val uri: Uri,
) : BookRenderer {

    private var pages: List<String> = emptyList()
    private var _tableOfContents: List<TocEntry> = emptyList()
    @Volatile private var initialized = false
    private val initMutex = Mutex()

    override val pageCount: Int get() = pages.size

    override val tableOfContents: List<TocEntry> get() = _tableOfContents

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initMutex.withLock {
            if (initialized) return@withLock
            runCatching {
                val (pageList, toc) = loadPages()
                pages = pageList
                _tableOfContents = toc
            }
            initialized = true
        }
    }

    private fun loadPages(): Pair<List<String>, List<TocEntry>> {
        val entries = context.contentResolver.openInputStream(uri)
            ?.let { readZipSafe(it) }
            ?: return Pair(emptyList(), emptyList())

        val containerXml = entries["META-INF/container.xml"] ?: return Pair(emptyList(), emptyList())
        val opfPath = parseContainerXml(containerXml) ?: return Pair(emptyList(), emptyList())
        val opfDir = opfPath.substringBeforeLast('/', "")
        val opfData = entries[opfPath] ?: return Pair(emptyList(), emptyList())
        val (spineHrefs, ncxHref) = parseOpfSpine(opfData)

        val pageList = spineHrefs.mapNotNull { href ->
            val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
            val cleanPath = fullPath.substringBefore('#')
            val html = entries[cleanPath]?.toString(Charsets.UTF_8) ?: return@mapNotNull null
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
        }.filter { it.isNotBlank() }

        // Build TOC from toc.ncx if available
        val tocEntries = if (ncxHref != null) {
            val ncxFullPath = if (opfDir.isEmpty()) ncxHref else "$opfDir/$ncxHref"
            val ncxData = entries[ncxFullPath]
            if (ncxData != null) {
                parseTocNcx(ncxData, spineHrefs, opfDir)
            } else emptyList()
        } else emptyList()

        return Pair(pageList, tocEntries)
    }

    private fun parseContainerXml(data: ByteArray): String? {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(data.inputStream(), null)
        }
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
            parser.next()
        }
        return null
    }

    private fun parseOpfSpine(data: ByteArray): Pair<List<String>, String?> {
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()
        var ncxHref: String? = null
        var ncxId: String? = null
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(data.inputStream(), null)
        }
        var inManifest = false
        var inSpine = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "manifest" -> inManifest = true
                    "spine" -> {
                        inManifest = false
                        inSpine = true
                        ncxId = parser.getAttributeValue(null, "toc")
                    }
                    "item" -> if (inManifest) {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val type = parser.getAttributeValue(null, "media-type") ?: ""
                        if (id != null && href != null) {
                            if ("xhtml" in type || "html" in type) {
                                manifest[id] = href
                            }
                            if (type == "application/x-dtbncx+xml" || id == "ncx" || id == "toc") {
                                ncxHref = href
                            }
                        }
                    }
                    "itemref" -> if (inSpine) {
                        val idref = parser.getAttributeValue(null, "idref")
                        if (idref != null) manifest[idref]?.let { spine.add(it) }
                    }
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                when (parser.name) {
                    "manifest" -> inManifest = false
                    "spine" -> inSpine = false
                }
            }
            parser.next()
        }
        // Also resolve ncx by ncxId if we didn't find it by media-type
        if (ncxHref == null && ncxId != null) {
            ncxHref = manifest[ncxId]
        }
        return Pair(spine, ncxHref)
    }

    private fun parseTocNcx(
        data: ByteArray,
        spineHrefs: List<String>,
        opfDir: String,
    ): List<TocEntry> {
        val result = mutableListOf<TocEntry>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(data.inputStream(), null)
        }
        var currentTitle: String? = null
        var inNavLabel = false
        var inText = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "navLabel" -> inNavLabel = true
                    "text" -> if (inNavLabel) inText = true
                    "content" -> {
                        val src = parser.getAttributeValue(null, "src")
                        if (src != null) {
                            val cleanSrc = src.substringBefore('#')
                            // Find which spine index this corresponds to
                            val pageIndex = spineHrefs.indexOfFirst { spineHref ->
                                val spineClean = spineHref.substringBefore('#')
                                // Compare by filename since opf dir prefixes may differ
                                spineClean.endsWith(cleanSrc) || cleanSrc.endsWith(spineClean) ||
                                    spineClean == cleanSrc
                            }
                            if (pageIndex >= 0 && currentTitle != null) {
                                result.add(TocEntry(title = currentTitle!!, pageIndex = pageIndex))
                            }
                            currentTitle = null
                            inNavLabel = false
                            inText = false
                        }
                    }
                }
                XmlPullParser.TEXT -> if (inText) {
                    currentTitle = parser.text?.trim()
                    inText = false
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "navLabel" -> inNavLabel = false
                }
            }
            parser.next()
        }
        return result
    }

    override suspend fun getPage(index: Int): PageContent = withContext(Dispatchers.IO) {
        ensureInitialized()
        val p = pages
        if (p.isEmpty()) return@withContext PageContent.Error("EPUB could not be read")
        if (index < 0 || index >= p.size) return@withContext PageContent.Error("Invalid page")
        PageContent.TextPage(p[index], index)
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

    override fun close() {}

    companion object {
        private fun readZipSafe(stream: InputStream): Map<String, ByteArray> {
            val result = mutableMapOf<String, ByteArray>()
            var totalBytes = 0L
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (result.size >= MAX_ENTRY_COUNT) break
                    if (!entry.isDirectory) {
                        val data = readEntryLimited(zip) ?: run {
                            zip.closeEntry()
                            entry = zip.nextEntry
                            return@use  // entry too large — abort entire archive
                        }
                        totalBytes += data.size
                        if (totalBytes > MAX_TOTAL_BYTES) break
                        result[entry.name] = data
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            return result
        }

        private fun readEntryLimited(zip: ZipInputStream): ByteArray? {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var entryBytes = 0L
            var n: Int
            while (zip.read(buf).also { n = it } >= 0) {
                entryBytes += n
                if (entryBytes > MAX_ENTRY_BYTES) return null
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }
}
