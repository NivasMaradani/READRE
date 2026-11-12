package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.MangaEntity
import com.example.data.entity.ReadingProgressEntity

@Database(
    entities = [
        ReadingProgressEntity::class,
        ChapterReadingProgressEntity::class,
        BookmarkEntity::class,
        AppSettingEntity::class,
        ChapterEntity::class,
        MangaEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {

    abstract fun readerDao(): ReaderDao

    companion object {
        @Volatile
        private var INSTANCE: ReaderDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `indexed_chapters` (
                        `mangaName` TEXT NOT NULL,
                        `chapterFileName` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `documentUri` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `lastScannedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mangaName`, `chapterFileName`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_indexed_chapters_mangaName` ON `indexed_chapters` (`mangaName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_indexed_chapters_mangaName_sortOrder` ON `indexed_chapters` (`mangaName`, `sortOrder`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `indexed_manga` (
                        `name` TEXT NOT NULL,
                        `folderUri` TEXT NOT NULL,
                        `chapterCount` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `lastScannedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`name`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chapter_reading_progress` (
                        `mangaName` TEXT NOT NULL,
                        `chapterFileName` TEXT NOT NULL,
                        `currentPageIndex` INTEGER NOT NULL,
                        `totalPages` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `lastReadTimestamp` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mangaName`, `chapterFileName`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_reading_progress_mangaName` ON `chapter_reading_progress` (`mangaName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_reading_progress_lastReadTimestamp` ON `chapter_reading_progress` (`lastReadTimestamp`)")

                // Migrate existing reading progress into chapter_reading_progress
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `chapter_reading_progress` (
                        `mangaName`, `chapterFileName`, `currentPageIndex`, `totalPages`, `isCompleted`, `lastReadTimestamp`, `updatedAt`
                    )
                    SELECT `mangaName`, `chapterFileName`, `pageIndex`, 0, 0, `timestamp`, `timestamp`
                    FROM `reading_progress`
                    """.trimIndent()
                )

                // Add customCoverPath and autoCoverPath columns to indexed_manga
                db.execSQL("ALTER TABLE `indexed_manga` ADD COLUMN `customCoverPath` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `indexed_manga` ADD COLUMN `autoCoverPath` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `indexed_manga` ADD COLUMN `isHidden` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ReaderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReaderDatabase::class.java,
                    "reader_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
