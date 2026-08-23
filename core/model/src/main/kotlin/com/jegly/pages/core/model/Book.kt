package com.jegly.pages.core.model

import kotlinx.datetime.Instant

enum class BookFormat {
    PDF, EPUB, TXT, MOBI, CBZ, CBR,
}

data class Book(
    val uri: String,
    val title: String,
    val authors: String,
    val coverUri: String?,
    val format: BookFormat,
    val fileSize: Long,
    val dateAdded: Instant,
    val lastOpened: Instant?,
    val currentPosition: String,
    val progressPercent: Float,
    val collectionId: Long? = null,
    val isEncrypted: Boolean = false,
)
