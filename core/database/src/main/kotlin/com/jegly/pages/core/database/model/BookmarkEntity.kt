package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jegly.pages.core.model.Bookmark
import kotlinx.datetime.Instant

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["uri"],
            childColumns = ["bookUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookUri")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookUri: String,
    val position: String,
    val note: String,
    val timestamp: Long,
)

fun BookmarkEntity.asExternalModel() = Bookmark(
    id = id,
    bookUri = bookUri,
    position = position,
    note = note,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

fun Bookmark.asEntity() = BookmarkEntity(
    id = id,
    bookUri = bookUri,
    position = position,
    note = note,
    timestamp = timestamp.toEpochMilliseconds(),
)
