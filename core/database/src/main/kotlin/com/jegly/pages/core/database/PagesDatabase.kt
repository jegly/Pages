package com.jegly.pages.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jegly.pages.core.database.dao.BookDao
import com.jegly.pages.core.database.dao.BookmarkDao
import com.jegly.pages.core.database.dao.CollectionDao
import com.jegly.pages.core.database.dao.HighlightDao
import com.jegly.pages.core.database.dao.TagDao
import com.jegly.pages.core.database.model.BookEntity
import com.jegly.pages.core.database.model.BookFtsEntity
import com.jegly.pages.core.database.model.BookmarkEntity
import com.jegly.pages.core.database.model.CollectionEntity
import com.jegly.pages.core.database.model.HighlightEntity
import com.jegly.pages.core.database.model.TagEntity

@Database(
    entities = [
        BookEntity::class,
        BookFtsEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        TagEntity::class,
        CollectionEntity::class,
    ],
    version = 3,
)
abstract class PagesDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )""",
                )
                db.execSQL("ALTER TABLE `books` ADD COLUMN `collectionId` INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `books` ADD COLUMN `isEncrypted` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
