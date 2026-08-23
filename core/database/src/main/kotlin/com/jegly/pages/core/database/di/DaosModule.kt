package com.jegly.pages.core.database.di

import com.jegly.pages.core.database.PagesDatabase
import com.jegly.pages.core.database.dao.BookDao
import com.jegly.pages.core.database.dao.BookmarkDao
import com.jegly.pages.core.database.dao.CollectionDao
import com.jegly.pages.core.database.dao.HighlightDao
import com.jegly.pages.core.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {

    @Provides
    fun providesBookDao(database: PagesDatabase): BookDao = database.bookDao()

    @Provides
    fun providesBookmarkDao(database: PagesDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun providesHighlightDao(database: PagesDatabase): HighlightDao = database.highlightDao()

    @Provides
    fun providesTagDao(database: PagesDatabase): TagDao = database.tagDao()

    @Provides
    fun providesCollectionDao(database: PagesDatabase): CollectionDao = database.collectionDao()
}
