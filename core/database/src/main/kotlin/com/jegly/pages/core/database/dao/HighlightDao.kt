package com.jegly.pages.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jegly.pages.core.database.model.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE bookUri = :bookUri ORDER BY pageIndex, startOffset")
    fun getHighlightsForBook(bookUri: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookUri = :bookUri AND pageIndex = :pageIndex ORDER BY startOffset")
    fun getHighlightsForPage(bookUri: String, pageIndex: Int): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("UPDATE highlights SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)
}
