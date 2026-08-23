package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = BookEntity::class)
@Entity(tableName = "booksFullTextSearch")
data class BookFtsEntity(
    val uri: String,
    val title: String,
    val authors: String,
)
