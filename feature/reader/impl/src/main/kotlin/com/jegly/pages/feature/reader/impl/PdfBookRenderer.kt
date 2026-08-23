package com.jegly.pages.feature.reader.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.jegly.pages.core.rendering.BookRenderer
import com.jegly.pages.core.rendering.PageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_BITMAP_PX = 4096
private const val MAX_BITMAP_HEIGHT = 8192  // guard against extreme PDF aspect ratios

class PdfBookRenderer(
    private val context: Context,
    private val uri: Uri,
) : BookRenderer {

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    @Volatile private var initialized = false
    private val initMutex = Mutex()
    private val pageMutex = Mutex()

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initMutex.withLock {
            if (initialized) return@withLock
            runCatching {
                pfd = if (uri.scheme == "file") {
                    // Decrypted temp file in cache — open directly without ContentResolver.
                    ParcelFileDescriptor.open(
                        java.io.File(uri.path!!),
                        ParcelFileDescriptor.MODE_READ_ONLY,
                    )
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")
                }
                renderer = pfd?.let { PdfRenderer(it) }
            }
            initialized = true
        }
    }

    override val pageCount: Int get() = renderer?.pageCount ?: 0

    // Set this to the screen pixel width before loading pages so each page bitmap is rendered
    // at exactly that width (height calculated from PDF aspect ratio). This eliminates all
    // Compose scaling — the bitmap IS the display size.
    @Volatile var targetWidth: Int = 0

    override suspend fun getPage(index: Int): PageContent = withContext(Dispatchers.IO) {
        ensureInitialized()
        val r = renderer ?: return@withContext PageContent.Error("PDF renderer unavailable")
        if (index < 0 || index >= r.pageCount) return@withContext PageContent.Error("Invalid page")

        pageMutex.withLock {
            val page = r.openPage(index)
            val pw = page.width.coerceAtLeast(1)
            val ph = page.height.coerceAtLeast(1)
            val tw = targetWidth
            val w: Int
            val h: Int
            if (tw > 0) {
                // Render at screen width; cap height so a maliciously extreme aspect ratio
                // (e.g. 1pt × 10,000,000pt) cannot allocate a multi-GB bitmap.
                val rawH = (tw.toLong() * ph / pw).toInt().coerceAtLeast(1)
                if (rawH > MAX_BITMAP_HEIGHT) {
                    h = MAX_BITMAP_HEIGHT
                    w = (MAX_BITMAP_HEIGHT.toLong() * pw / ph).toInt().coerceAtLeast(1)
                } else {
                    w = tw
                    h = rawH
                }
            } else {
                w = (pw * 2).coerceAtMost(MAX_BITMAP_PX)
                h = (ph * 2).coerceAtMost(MAX_BITMAP_PX)
            }
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            PageContent.BitmapPage(bitmap, index)
        }
    }

    override fun close() {
        renderer?.close()
        pfd?.close()
    }
}
