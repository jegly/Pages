package com.jegly.pages.core.model

import kotlinx.datetime.Instant

data class Bookmark(
    val id: Long,
    val bookUri: String,
    val position: String,
    val note: String,
    val timestamp: Instant,
)
