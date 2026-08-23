package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.jegly.pages.core.model.Tag

@Entity(
    tableName = "tags",
    primaryKeys = ["bookUri", "tagName"],
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
data class TagEntity(
    val bookUri: String,
    val tagName: String,
)

fun TagEntity.asExternalModel() = Tag(bookUri = bookUri, tagName = tagName)
fun Tag.asEntity() = TagEntity(bookUri = bookUri, tagName = tagName)
