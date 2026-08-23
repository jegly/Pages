package com.jegly.pages.core.testing.data

import com.jegly.pages.core.model.Book
import com.jegly.pages.core.model.BookFormat
import kotlinx.datetime.Instant

val testBooks = listOf(
    Book(
        uri = "content://media/external/documents/1",
        title = "The Pragmatic Programmer",
        authors = "David Thomas, Andrew Hunt",
        coverUri = null,
        format = BookFormat.PDF,
        fileSize = 2_500_000L,
        dateAdded = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        lastOpened = Instant.fromEpochMilliseconds(1_710_000_000_000L),
        currentPosition = "42",
        progressPercent = 0.35f,
    ),
    Book(
        uri = "content://media/external/documents/2",
        title = "Clean Code",
        authors = "Robert C. Martin",
        coverUri = null,
        format = BookFormat.EPUB,
        fileSize = 1_200_000L,
        dateAdded = Instant.fromEpochMilliseconds(1_700_500_000_000L),
        lastOpened = null,
        currentPosition = "0",
        progressPercent = 0.0f,
    ),
)
