package com.jegly.pages.core.database.di

import android.content.Context
import androidx.room.Room
import com.jegly.pages.core.database.PagesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesPagesDatabase(
        @ApplicationContext context: Context,
    ): PagesDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory("pages-secure-db".toByteArray())
        return Room.databaseBuilder(
            context,
            PagesDatabase::class.java,
            "pages-database",
        )
            .openHelperFactory(factory)
            .addMigrations(PagesDatabase.MIGRATION_1_2, PagesDatabase.MIGRATION_2_3)
            .build()
    }
}
