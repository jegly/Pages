package com.jegly.pages.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jegly.pages.core.database.model.BookEntity
import com.jegly.pages.core.model.BookFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest : DatabaseTest() {

    @Test
    fun insertAndRetrieveBook() = runTest {
        val book = sampleEntity("content://test/book1", "Test Book")
        bookDao.insertBook(book)
        val result = bookDao.getAllBooks().first()
        assertEquals(1, result.size)
        assertEquals("Test Book", result[0].title)
    }

    @Test
    fun getBookByUri() = runTest {
        val book = sampleEntity("content://test/book2", "Another Book", BookFormat.PDF)
        bookDao.insertBook(book)
        val result = bookDao.getBook("content://test/book2").first()
        assertEquals("Another Book", result?.title)
    }

    @Test
    fun getBookReturnsNullForMissingUri() = runTest {
        val result = bookDao.getBook("content://nonexistent").first()
        assertNull(result)
    }

    @Test
    fun updateProgressPersists() = runTest {
        bookDao.insertBook(sampleEntity("content://test/book3", "Progress Book"))
        bookDao.updateProgress("content://test/book3", "page:5", 0.5f, System.currentTimeMillis())
        val result = bookDao.getBook("content://test/book3").first()
        assertEquals("page:5", result?.currentPosition)
        assertEquals(0.5f, result?.progressPercent)
    }

    private fun sampleEntity(
        uri: String,
        title: String,
        format: BookFormat = BookFormat.TXT,
    ) = BookEntity(
        uri = uri,
        title = title,
        authors = "Author",
        coverUri = null,
        format = format,
        fileSize = 1024L,
        dateAdded = System.currentTimeMillis(),
        lastOpened = null,
        currentPosition = "0",
        progressPercent = 0f,
    )
}
