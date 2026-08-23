package com.jegly.pages.core.model

import kotlinx.datetime.Instant

data class Collection(
    val id: Long = 0L,
    val name: String,
    val createdAt: Instant,
)
