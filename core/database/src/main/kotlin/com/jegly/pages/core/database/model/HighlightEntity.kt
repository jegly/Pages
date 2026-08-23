package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jegly.pages.core.model.Highlight
import kotlinx.datetime.Instant

@Entity(
    tableName = "highlights",
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
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookUri: String,
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val colorArgb: Long,
    val note: String,
    val timestamp: Long,
)

fun HighlightEntity.asExternalModel() = Highlight(
    id = id,
    bookUri = bookUri,
    pageIndex = pageIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    colorArgb = colorArgb,
    note = note,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

fun Highlight.asEntity() = HighlightEntity(
    id = id,
    bookUri = bookUri,
    pageIndex = pageIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    colorArgb = colorArgb,
    note = note,
    timestamp = timestamp.toEpochMilliseconds(),
)
