package com.jegly.pages.core.data.repository

import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.Bookmark
import com.jegly.pages.core.testing.repository.TestPagesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineFirstPagesRepositoryTest {

    private lateinit var subject: TestPagesRepository

    @Before
    fun setUp() {
        subject = TestPagesRepository()
    }

    @Test
    fun booksEmptyByDefault() = runTest {
        assertTrue(subject.getAllBooks().first().isEmpty())
    }

    @Test
    fun upsertAndRetrieveBook() = runTest {
        val book = sampleBook("content://test/1")
        subject.upsertBook(book)
        val result = subject.getAllBooks().first()
        assertEquals(1, result.size)
        assertEquals("content://test/1", result[0].uri)
    }

    @Test
    fun deleteBookRemovesIt() = runTest {
        val book = sampleBook("content://test/2")
        subject.upsertBook(book)
        subject.deleteBook("content://test/2")
        assertTrue(subject.getAllBooks().first().isEmpty())
    }

    @Test
    fun bookmarkRoundtrip() = runTest {
        val bookmark = Bookmark(
            id = 0,
            bookUri = "content://test/3",
            position = "page:10",
            note = "great scene",
            timestamp = Clock.System.now(),
        )
        subject.addBookmark(bookmark)
        val result = subject.getAllBookmarks().first()
        assertEquals(1, result.size)
        assertEquals("page:10", result[0].position)
    }

    @Test
    fun tagRoundtrip() = runTest {
        subject.addTag("content://test/4", "fantasy")
        val result = subject.getTagsForBook("content://test/4").first()
        assertEquals(listOf("fantasy"), result.map { it.tagName })
    }

    @Test
    fun setAccentColorPersists() = runTest {
        subject.setAccentColor(AccentColor.Amber)
        assertEquals(AccentColor.Amber, subject.getUserPreferences().first().accentColor)
    }

    private fun sampleBook(uri: String) = Book(
        uri = uri,
        title = "Sample Book",
        authors = "Author",
        format = BookFormat.TXT,
        fileSize = 1024L,
        dateAdded = Clock.System.now(),
    )
}
