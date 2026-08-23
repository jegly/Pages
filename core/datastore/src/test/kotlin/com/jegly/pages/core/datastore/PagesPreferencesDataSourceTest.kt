package com.jegly.pages.core.datastore

import android.content.SharedPreferences
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.ReadingTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PagesPreferencesDataSourceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var subject: PagesPreferencesDataSource

    @Before
    fun setUp() {
        sharedPreferences = InMemorySharedPreferences()
        subject = PagesPreferencesDataSource(sharedPreferences)
    }

    @Test
    fun defaultPreferencesAreCorrect() = testScope.runTest {
        val prefs = subject.userPreferences.first()
        assertEquals(AccentColor.Sapphire, prefs.accentColor)
        assertEquals(1.0f, prefs.fontScaleFactor)
        assertEquals(ReadingTheme.System, prefs.readingTheme)
        assertEquals(false, prefs.useDynamicColor)
        assertEquals(null, prefs.lastOpenedBookUri)
    }

    @Test
    fun setAccentColorPersists() = testScope.runTest {
        subject.setAccentColor(AccentColor.Emerald)
        assertEquals(AccentColor.Emerald, subject.userPreferences.first().accentColor)
    }

    @Test
    fun setFontScalePersists() = testScope.runTest {
        subject.setFontScaleFactor(1.5f)
        assertEquals(1.5f, subject.userPreferences.first().fontScaleFactor)
    }

    @Test
    fun setReadingThemePersists() = testScope.runTest {
        subject.setReadingTheme(ReadingTheme.Dark)
        assertEquals(ReadingTheme.Dark, subject.userPreferences.first().readingTheme)
    }

    @Test
    fun setLastOpenedBookUriPersists() = testScope.runTest {
        subject.setLastOpenedBookUri("content://test/book1")
        assertEquals("content://test/book1", subject.userPreferences.first().lastOpenedBookUri)
    }

    @Test
    fun clearLastOpenedBookUri() = testScope.runTest {
        subject.setLastOpenedBookUri("content://test/book1")
        subject.setLastOpenedBookUri(null)
        assertEquals(null, subject.userPreferences.first().lastOpenedBookUri)
    }
}
