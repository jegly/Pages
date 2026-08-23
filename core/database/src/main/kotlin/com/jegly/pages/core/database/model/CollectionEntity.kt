package com.jegly.pages.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jegly.pages.core.model.Collection
import kotlinx.datetime.Instant

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
)

fun CollectionEntity.asExternalModel() = Collection(
    id = id,
    name = name,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

fun Collection.asEntity() = CollectionEntity(
    id = id,
    name = name,
    createdAt = createdAt.toEpochMilliseconds(),
)
