package com.jegly.pages.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jegly.pages.core.database.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpened DESC NULLS LAST, dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE uri = :uri")
    fun getBook(uri: String): Flow<BookEntity?>

    @Query("SELECT * FROM books ORDER BY lastOpened DESC NULLS LAST LIMIT :limit")
    fun getRecentBooks(limit: Int): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE collectionId = :collectionId ORDER BY lastOpened DESC NULLS LAST, dateAdded DESC")
    fun getBooksByCollection(collectionId: Long): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query(
        """UPDATE books SET currentPosition = :position, progressPercent = :percent, lastOpened = :nowMs
           WHERE uri = :uri""",
    )
    suspend fun updateProgress(uri: String, position: String, percent: Float, nowMs: Long)

    @Query("UPDATE books SET collectionId = :collectionId WHERE uri = :bookUri")
    suspend fun moveBookToCollection(bookUri: String, collectionId: Long?)

    @Query("DELETE FROM books WHERE uri = :uri")
    suspend fun deleteBook(uri: String)

    @Query(
        """SELECT books.* FROM books
           INNER JOIN booksFullTextSearch ON books.rowid = booksFullTextSearch.rowid
           WHERE booksFullTextSearch MATCH :query""",
    )
    fun searchBooks(query: String): Flow<List<BookEntity>>
}
