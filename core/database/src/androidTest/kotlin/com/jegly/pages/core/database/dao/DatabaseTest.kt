package com.jegly.pages.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jegly.pages.core.database.PagesDatabase
import org.junit.After
import org.junit.Before

internal abstract class DatabaseTest {

    private lateinit var db: PagesDatabase
    protected lateinit var bookDao: BookDao
    protected lateinit var bookmarkDao: BookmarkDao
    protected lateinit var tagDao: TagDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PagesDatabase::class.java).build()
        bookDao = db.bookDao()
        bookmarkDao = db.bookmarkDao()
        tagDao = db.tagDao()
    }

    @After
    fun teardown() = db.close()
}
