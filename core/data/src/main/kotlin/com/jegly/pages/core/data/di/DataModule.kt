package com.jegly.pages.core.data.di

import com.jegly.pages.core.data.repository.OfflineFirstPagesRepository
import com.jegly.pages.core.data.repository.PagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsPagesRepository(
        impl: OfflineFirstPagesRepository,
    ): PagesRepository
}
