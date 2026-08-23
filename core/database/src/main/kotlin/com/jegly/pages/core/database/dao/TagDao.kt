package com.jegly.pages.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jegly.pages.core.database.model.BookEntity
import com.jegly.pages.core.database.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags WHERE bookUri = :bookUri")
    fun getTagsForBook(bookUri: String): Flow<List<TagEntity>>

    @Query("SELECT DISTINCT tagName FROM tags ORDER BY tagName ASC")
    fun getAllTagNames(): Flow<List<String>>

    @Query(
        """SELECT books.* FROM books
           INNER JOIN tags ON books.uri = tags.bookUri
           WHERE tags.tagName = :tagName""",
    )
    fun getBooksByTag(tagName: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE bookUri = :bookUri AND tagName = :tagName")
    suspend fun deleteTag(bookUri: String, tagName: String)
}
