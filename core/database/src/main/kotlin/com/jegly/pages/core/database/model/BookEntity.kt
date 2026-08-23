package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import kotlinx.datetime.Instant

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val authors: String,
    val coverUri: String?,
    val format: BookFormat,
    val fileSize: Long,
    val dateAdded: Long,
    val lastOpened: Long?,
    val currentPosition: String,
    val progressPercent: Float,
    val collectionId: Long? = null,
    val isEncrypted: Boolean = false,
)

fun BookEntity.asExternalModel() = Book(
    uri = uri,
    title = title,
    authors = authors,
    coverUri = coverUri,
    format = format,
    fileSize = fileSize,
    dateAdded = Instant.fromEpochMilliseconds(dateAdded),
    lastOpened = lastOpened?.let { Instant.fromEpochMilliseconds(it) },
    currentPosition = currentPosition,
    progressPercent = progressPercent,
    collectionId = collectionId,
    isEncrypted = isEncrypted,
)

fun Book.asEntity() = BookEntity(
    uri = uri,
    title = title,
    authors = authors,
    coverUri = coverUri,
    format = format,
    fileSize = fileSize,
    dateAdded = dateAdded.toEpochMilliseconds(),
    lastOpened = lastOpened?.toEpochMilliseconds(),
    currentPosition = currentPosition,
    progressPercent = progressPercent,
    collectionId = collectionId,
    isEncrypted = isEncrypted,
)
