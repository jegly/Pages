package com.jegly.pages.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Xml
import com.jegly.pages.core.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024   // 10 MB per image entry
private const val MAX_XML_BYTES = 2L * 1024 * 1024      // 2 MB per XML/OPF entry

suspend fun extractCover(context: Context, uri: Uri, format: BookFormat): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
            val hash = uri.toString().hashCode().toString()
            val outFile = File(coversDir, "$hash.jpg")

            when (format) {
                BookFormat.EPUB -> extractEpubCover(context, uri, outFile)
                BookFormat.PDF -> extractPdfCover(context, uri, outFile)
                BookFormat.CBZ -> extractCbzCover(context, uri, outFile)
                else -> null
            }
        }.getOrNull()
    }

private fun extractEpubCover(context: Context, uri: Uri, outFile: File): String? {
    val entries = context.contentResolver.openInputStream(uri)
        ?.let { readZipForCover(it) }
        ?: return null

    val containerXml = entries["META-INF/container.xml"] ?: return null
    val opfPath = parseContainerXmlForCover(containerXml) ?: return null
    val opfDir = opfPath.substringBeforeLast('/', "")
    val opfData = entries[opfPath] ?: return null

    val coverHref = parseOpfManifestForCover(opfData) ?: return null
    val fullPath = if (opfDir.isEmpty()) coverHref else "$opfDir/$coverHref"
    val coverBytes = entries[fullPath] ?: entries[coverHref] ?: return null

    FileOutputStream(outFile).use { it.write(coverBytes) }
    return outFile.absolutePath
}

private fun parseContainerXmlForCover(data: ByteArray): String? {
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

private fun parseOpfManifestForCover(data: ByteArray): String? {
    data class ManifestItem(val id: String, val href: String, val mediaType: String, val properties: String)

    val items = mutableListOf<ManifestItem>()
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setInput(data.inputStream(), null)
    }
    var inManifest = false
    var coverMetaId: String? = null
    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.START_TAG) {
            when (parser.name) {
                "manifest" -> inManifest = true
                "item" -> if (inManifest) {
                    val id = parser.getAttributeValue(null, "id") ?: ""
                    val href = parser.getAttributeValue(null, "href") ?: ""
                    val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                    val properties = parser.getAttributeValue(null, "properties") ?: ""
                    items.add(ManifestItem(id, href, mediaType, properties))
                }
                "meta" -> {
                    val name = parser.getAttributeValue(null, "name")
                    val content = parser.getAttributeValue(null, "content")
                    if (name == "cover" && content != null) coverMetaId = content
                }
            }
        } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "manifest") {
            inManifest = false
        }
        parser.next()
    }

    val byProperties = items.firstOrNull { it.properties == "cover-image" && it.mediaType.startsWith("image/") }
    if (byProperties != null) return byProperties.href

    if (coverMetaId != null) {
        val byMeta = items.firstOrNull { it.id == coverMetaId && it.mediaType.startsWith("image/") }
        if (byMeta != null) return byMeta.href
    }

    return items.firstOrNull { "cover" in it.id.lowercase() && it.mediaType.startsWith("image/") }?.href
}

// Only buffers entries we actually need for cover extraction: XML/OPF metadata and images.
// Skips HTML, CSS, fonts, and other large content entries entirely.
private fun readZipForCover(stream: InputStream): Map<String, ByteArray> {
    val result = mutableMapOf<String, ByteArray>()
    ZipInputStream(stream).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val name = entry.name
                val ext = name.substringAfterLast('.').lowercase()
                val isXml = ext == "xml" || ext == "opf" || name.startsWith("META-INF/")
                val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp")
                if (isXml || isImage) {
                    val limit = if (isImage) MAX_IMAGE_BYTES else MAX_XML_BYTES
                    val buf = ByteArrayOutputStream()
                    val temp = ByteArray(8192)
                    var entryBytes = 0L
                    var n: Int
                    var tooLarge = false
                    while (zip.read(temp).also { n = it } >= 0) {
                        entryBytes += n
                        if (entryBytes > limit) { tooLarge = true; break }
                        buf.write(temp, 0, n)
                    }
                    if (!tooLarge) result[name] = buf.toByteArray()
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return result
}

private val CBZ_IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp")

private fun extractCbzCover(context: Context, uri: Uri, outFile: File): String? {
    val candidates = mutableMapOf<String, ByteArray>()
    context.contentResolver.openInputStream(uri)?.use { stream ->
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null && candidates.size < 5) {
                if (!entry.isDirectory) {
                    val ext = entry.name.substringAfterLast('.').lowercase()
                    if (ext in CBZ_IMAGE_EXTS) {
                        val buf = ByteArrayOutputStream()
                        val tmp = ByteArray(8192)
                        var bytes = 0L
                        var n: Int
                        while (zip.read(tmp).also { n = it } >= 0) {
                            bytes += n
                            if (bytes > MAX_IMAGE_BYTES) break
                            buf.write(tmp, 0, n)
                        }
                        if (bytes <= MAX_IMAGE_BYTES) candidates[entry.name] = buf.toByteArray()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    val data = candidates.entries.minByOrNull { it.key.lowercase() }?.value ?: return null
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
    FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    bitmap.recycle()
    return outFile.absolutePath
}

private fun extractPdfCover(context: Context, uri: Uri, outFile: File): String? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        renderer = PdfRenderer(pfd)
        if (renderer.pageCount == 0) return null
        val page = renderer.openPage(0)

        val pageW = page.width.coerceAtLeast(1)
        val pageH = page.height.coerceAtLeast(1)
        val scale = minOf(300f / pageW, 400f / pageH)
        val bitmapW = (pageW * scale).toInt().coerceAtLeast(1)
        val bitmapH = (pageH * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        FileOutputStream(outFile).use { fos -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos) }
        bitmap.recycle()
        outFile.absolutePath
    } finally {
        renderer?.close()
        pfd?.close()
    }
}
