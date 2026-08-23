package com.jegly.pages.core.rendering

import android.graphics.Bitmap

sealed interface PageContent {
    data class BitmapPage(val bitmap: Bitmap, val pageIndex: Int) : PageContent
    data class TextPage(val text: String, val pageIndex: Int) : PageContent
    data object Loading : PageContent
    data class Error(val message: String) : PageContent
}

data class TocEntry(val title: String, val pageIndex: Int)

data class SearchResult(val pageIndex: Int, val snippet: String)

interface BookRenderer {
    val pageCount: Int
    suspend fun getPage(index: Int): PageContent
    fun close()
    val tableOfContents: List<TocEntry> get() = emptyList()
    suspend fun searchPages(query: String): List<SearchResult> = emptyList()
}
