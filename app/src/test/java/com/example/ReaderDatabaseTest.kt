package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderDatabaseTest {

    private lateinit var database: ReaderDatabase
    private lateinit var dao: ReaderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.readerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndRetrieveReadingProgress() {
        runBlocking {
            val progress = ReadingProgressEntity(
                mangaName = "One Piece",
                chapterFileName = "Chapter 1095.cbz",
                pageIndex = 14,
                timestamp = 1000L
            )
            dao.saveReadingProgress(progress)

            val retrieved = dao.getProgressForMangaSync("One Piece")
            assertNotNull(retrieved)
            assertEquals("Chapter 1095.cbz", retrieved?.chapterFileName)
            assertEquals(14, retrieved?.pageIndex)

            // Update reading position for same manga
            val updated = ReadingProgressEntity(
                mangaName = "One Piece",
                chapterFileName = "Chapter 1096.cbz",
                pageIndex = 2,
                timestamp = 2000L
            )
            dao.saveReadingProgress(updated)

            val all = dao.getAllReadingProgress().first()
            assertEquals(1, all.size)
            assertEquals("Chapter 1096.cbz", all[0].chapterFileName)
            assertEquals(2, all[0].pageIndex)
        }
    }

    @Test
    fun testAddQueryAndRemoveBookmarksWithoutAffectingProgress() {
        runBlocking {
            // Save reading progress
            dao.saveReadingProgress(
                ReadingProgressEntity("Solo Leveling", "Chapter 100.zip", 5, 1000L)
            )

            // Add bookmarks
            val b1 = BookmarkEntity(mangaName = "Solo Leveling", chapterFileName = "Chapter 45.zip", timestamp = 500L)
            val b2 = BookmarkEntity(mangaName = "Solo Leveling", chapterFileName = "Chapter 110.zip", timestamp = 600L)
            val b3 = BookmarkEntity(mangaName = "One Piece", chapterFileName = "Chapter 1044.cbz", timestamp = 700L)

            dao.insertBookmark(b1)
            dao.insertBookmark(b2)
            dao.insertBookmark(b3)

            // Verify bookmark existence
            assertTrue(dao.isChapterBookmarkedSync("Solo Leveling", "Chapter 45.zip"))
            assertTrue(dao.isChapterBookmarkedSync("One Piece", "Chapter 1044.cbz"))
            assertFalse(dao.isChapterBookmarkedSync("Solo Leveling", "Chapter 99.zip"))

            // Verify grouping
            val soloBookmarks = dao.getBookmarksForManga("Solo Leveling").first()
            assertEquals(2, soloBookmarks.size)

            // Remove a bookmark
            dao.deleteBookmark("Solo Leveling", "Chapter 45.zip")
            assertFalse(dao.isChapterBookmarkedSync("Solo Leveling", "Chapter 45.zip"))

            // Verify reading progress is completely unaffected
            val progress = dao.getProgressForMangaSync("Solo Leveling")
            assertNotNull(progress)
            assertEquals("Chapter 100.zip", progress?.chapterFileName)
            assertEquals(5, progress?.pageIndex)
        }
    }

    @Test
    fun testAppSettingsPersistence() {
        runBlocking {
            dao.saveSetting(AppSettingEntity("reader_folder_uri", "content://com.android.externalstorage/tree/Reader"))
            dao.saveSetting(AppSettingEntity("default_reading_mode", "VERTICAL_CONTINUOUS"))
            dao.saveSetting(AppSettingEntity("default_reading_direction", "RIGHT_TO_LEFT"))

            assertEquals("content://com.android.externalstorage/tree/Reader", dao.getSettingSync("reader_folder_uri"))
            assertEquals("VERTICAL_CONTINUOUS", dao.getSettingSync("default_reading_mode"))
            assertEquals("RIGHT_TO_LEFT", dao.getSettingSync("default_reading_direction"))
        }
    }
}
