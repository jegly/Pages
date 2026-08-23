package com.jegly.pages.core.model

import kotlinx.datetime.Instant

data class Highlight(
    val id: Long,
    val bookUri: String,
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val colorArgb: Long,
    val note: String,
    val timestamp: Instant,
)
