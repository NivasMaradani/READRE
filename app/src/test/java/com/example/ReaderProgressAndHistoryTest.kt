package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.data.repository.ReaderRepository
import com.example.util.CoverManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderProgressAndHistoryTest {

    private lateinit var context: Context
    private lateinit var database: ReaderDatabase
    private lateinit var dao: ReaderDao
    private lateinit var repository: ReaderRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.readerDao()
        repository = ReaderRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Scenario A: Room Migration 2 -> 3 ---
    @Test
    fun testScenarioA_migrationV2ToV3_preservesProgressAndBookmarks() {
        // Create an open helper to build a Version 2 SQLite schema directly
        val dbFile = File(context.cacheDir, "migration_test.db")
        if (dbFile.exists()) dbFile.delete()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `reading_progress` (
                            `mangaName` TEXT NOT NULL,
                            `chapterFileName` TEXT NOT NULL,
                            `pageIndex` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            PRIMARY KEY(`mangaName`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `bookmarks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `mangaName` TEXT NOT NULL,
                            `chapterFileName` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
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
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `app_settings` (
                            `key` TEXT NOT NULL,
                            `value` TEXT NOT NULL,
                            PRIMARY KEY(`key`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val v2Db = helper.writableDatabase

        // Insert test data in V2
        v2Db.execSQL("INSERT INTO reading_progress VALUES ('Jujutsu Kaisen', 'Chapter 250.cbz', 12, 1700000000)")
        v2Db.execSQL("INSERT INTO bookmarks (mangaName, chapterFileName, timestamp) VALUES ('Jujutsu Kaisen', 'Chapter 250.cbz', 1700000000)")
        v2Db.execSQL("INSERT INTO indexed_manga VALUES ('Jujutsu Kaisen', 'content://test', 50, 0, 1700000000)")

        // Apply MIGRATION_2_3
        ReaderDatabase.MIGRATION_2_3.migrate(v2Db)

        // Verify chapter_reading_progress exists and has migrated row
        val cursor = v2Db.query("SELECT mangaName, chapterFileName, currentPageIndex, isCompleted FROM chapter_reading_progress WHERE mangaName = 'Jujutsu Kaisen'")
        assertTrue("Migrated row must exist", cursor.moveToFirst())
        assertEquals("Jujutsu Kaisen", cursor.getString(0))
        assertEquals("Chapter 250.cbz", cursor.getString(1))
        assertEquals(12, cursor.getInt(2))
        assertEquals(0, cursor.getInt(3)) // isCompleted = 0
        cursor.close()

        // Verify bookmarks are unchanged
        val bookmarkCursor = v2Db.query("SELECT mangaName, chapterFileName FROM bookmarks WHERE mangaName = 'Jujutsu Kaisen'")
        assertTrue(bookmarkCursor.moveToFirst())
        assertEquals("Chapter 250.cbz", bookmarkCursor.getString(1))
        bookmarkCursor.close()

        // Verify columns added to indexed_manga
        val mangaCursor = v2Db.query("SELECT customCoverPath, autoCoverPath FROM indexed_manga WHERE name = 'Jujutsu Kaisen'")
        assertTrue(mangaCursor.moveToFirst())
        assertNull(mangaCursor.getString(0))
        assertNull(mangaCursor.getString(1))
        mangaCursor.close()

        v2Db.close()
        dbFile.delete()
    }

    // --- Scenario B: Independent Chapter Progress ---
    @Test
    fun testScenarioB_independentChapterProgress() = runBlocking {
        // Read Chapter 15
        repository.saveChapterProgress(
            mangaName = "Bleach",
            chapterFileName = "Chapter 15.zip",
            pageIndex = 14,
            totalPages = 25,
            isCompleted = false
        )

        // Read Chapter 105
        repository.saveChapterProgress(
            mangaName = "Bleach",
            chapterFileName = "Chapter 105.zip",
            pageIndex = 4,
            totalPages = 20,
            isCompleted = false
        )

        // Verify Chapter 15 progress is preserved independently
        val ch15 = repository.getChapterProgress("Bleach", "Chapter 15.zip")
        assertNotNull(ch15)
        assertEquals(14, ch15?.currentPageIndex)
        assertEquals(25, ch15?.totalPages)
        assertFalse(ch15?.isCompleted ?: true)

        // Verify Chapter 105 progress is preserved independently
        val ch105 = repository.getChapterProgress("Bleach", "Chapter 105.zip")
        assertNotNull(ch105)
        assertEquals(4, ch105?.currentPageIndex)
        assertEquals(20, ch105?.totalPages)
        assertFalse(ch105?.isCompleted ?: true)

        // Both chapters must exist in manga's progress list
        val allBleach = dao.getChapterProgressForMangaSync("Bleach")
        assertEquals(2, allBleach.size)
    }

    // --- Scenario C: Page Progress Persistence & Exit Flush ---
    @Test
    fun testScenarioC_pageProgressPersistence() = runBlocking {
        repository.saveChapterProgress(
            mangaName = "Naruto",
            chapterFileName = "Chapter 1.cbz",
            pageIndex = 0,
            totalPages = 30
        )

        // Simulate reading pages progressively
        repository.saveChapterProgress(
            mangaName = "Naruto",
            chapterFileName = "Chapter 1.cbz",
            pageIndex = 5,
            totalPages = 30
        )

        repository.saveChapterProgress(
            mangaName = "Naruto",
            chapterFileName = "Chapter 1.cbz",
            pageIndex = 15,
            totalPages = 30
        )

        val retrieved = repository.getChapterProgress("Naruto", "Chapter 1.cbz")
        assertNotNull(retrieved)
        assertEquals(15, retrieved?.currentPageIndex)
        assertEquals(30, retrieved?.totalPages)
    }

    // --- Scenario D: Auto-completion on Reaching Last Page ---
    @Test
    fun testScenarioD_autoCompletionOnLastPage() = runBlocking {
        // Total pages = 20 (indices 0..19)
        repository.saveChapterProgress(
            mangaName = "Dragon Ball",
            chapterFileName = "Chapter 1.zip",
            pageIndex = 18,
            totalPages = 20,
            isCompleted = false
        )
        assertFalse(repository.getChapterProgress("Dragon Ball", "Chapter 1.zip")?.isCompleted ?: true)

        // User advances to last page (index 19)
        repository.saveChapterProgress(
            mangaName = "Dragon Ball",
            chapterFileName = "Chapter 1.zip",
            pageIndex = 19,
            totalPages = 20,
            isCompleted = false // Should automatically become true
        )

        val completed = repository.getChapterProgress("Dragon Ball", "Chapter 1.zip")
        assertNotNull(completed)
        assertTrue("Chapter must be auto-completed on reaching last page", completed?.isCompleted ?: false)
    }

    // --- Scenario E: Manual Mark as Completed & Mark as Unread ---
    @Test
    fun testScenarioE_manualMarkCompletedAndUnread() = runBlocking {
        repository.saveChapterProgress(
            mangaName = "Hunter x Hunter",
            chapterFileName = "Chapter 300.cbz",
            pageIndex = 5,
            totalPages = 24,
            isCompleted = false
        )

        // Manual mark as completed
        repository.markChapterCompleted("Hunter x Hunter", "Chapter 300.cbz", 24)
        val completed = repository.getChapterProgress("Hunter x Hunter", "Chapter 300.cbz")
        assertNotNull(completed)
        assertTrue(completed?.isCompleted ?: false)
        assertEquals(23, completed?.currentPageIndex) // points to last page

        // Manual mark as unread
        repository.markChapterUnread("Hunter x Hunter", "Chapter 300.cbz")
        val unread = repository.getChapterProgress("Hunter x Hunter", "Chapter 300.cbz")
        assertNotNull(unread)
        assertFalse(unread?.isCompleted ?: true)
        assertEquals(0, unread?.currentPageIndex) // reset to 0
    }

    // --- Scenario F: Reading History Ordering ---
    @Test
    fun testScenarioF_readingHistoryOrdering() = runBlocking {
        val t1 = 1000000L
        val t2 = 2000000L
        val t3 = 3000000L

        dao.saveChapterProgress(
            ChapterReadingProgressEntity("Manga X", "Ch 1.zip", 5, 20, false, lastReadTimestamp = t1)
        )
        dao.saveChapterProgress(
            ChapterReadingProgressEntity("Manga X", "Ch 2.zip", 2, 20, false, lastReadTimestamp = t3)
        )
        dao.saveChapterProgress(
            ChapterReadingProgressEntity("Manga Y", "Ch 5.zip", 10, 20, true, lastReadTimestamp = t2)
        )

        val history = repository.getAllChapterHistorySync()
        assertEquals(3, history.size)
        // Should be ordered descending: t3, t2, t1
        assertEquals("Ch 2.zip", history[0].chapterFileName)
        assertEquals("Ch 5.zip", history[1].chapterFileName)
        assertEquals("Ch 1.zip", history[2].chapterFileName)
    }

    // --- Scenario G: Continue Reading Manga-Level Pointer ---
    @Test
    fun testScenarioG_continueReadingMangaPointer() = runBlocking {
        // Read chapter 1
        repository.saveChapterProgress("Vinland Saga", "Chapter 1.cbz", 10, 25)
        var pointer = repository.getProgressForManga("Vinland Saga")
        assertNotNull(pointer)
        assertEquals("Chapter 1.cbz", pointer?.chapterFileName)
        assertEquals(10, pointer?.pageIndex)

        // Read chapter 2
        repository.saveChapterProgress("Vinland Saga", "Chapter 2.cbz", 3, 22)
        pointer = repository.getProgressForManga("Vinland Saga")
        assertNotNull(pointer)
        assertEquals("Chapter 2.cbz", pointer?.chapterFileName)
        assertEquals(3, pointer?.pageIndex)

        // Manga list contains only 1 entry for this manga in reading_progress
        val allProgress = dao.getAllReadingProgress().first()
        assertEquals(1, allProgress.filter { it.mangaName == "Vinland Saga" }.size)
    }

    // --- Scenario H: Cover Thumbnail Generation with Downsampling ---
    @Test
    fun testScenarioH_coverThumbnailGeneration() {
        runBlocking {
            val testArchive = File(context.cacheDir, "test_manga_chapter.zip")
            val zos = ZipOutputStream(FileOutputStream(testArchive))

            // Create a large dummy bitmap (800 x 1200)
            val bitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.RGB_565)
            val tempImg = File(context.cacheDir, "temp_img.jpg")
            FileOutputStream(tempImg).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()

            zos.putNextEntry(ZipEntry("001_cover.jpg"))
            tempImg.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
            zos.close()
            tempImg.delete()

            val coverFile = CoverManager.getOrGenerateAutoCover(
                context = context,
                mangaName = "Berserk",
                firstChapterUri = Uri.fromFile(testArchive)
            )

            assertNotNull("Generated cover should not be null", coverFile)
            assertTrue("Cover file must exist", coverFile?.exists() ?: false)
            assertTrue("Cover file must not be empty", (coverFile?.length() ?: 0L) > 0L)

            // Verify bounds are downsampled
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(coverFile?.absolutePath, options)
            assertTrue("Width should be downsampled", options.outWidth <= 480)
            assertTrue("Height should be downsampled", options.outHeight <= 600)

            testArchive.delete()
        }
    }

    // --- Scenario I: Custom Cover Replacement & Restoration ---
    @Test
    fun testScenarioI_customCoverReplacementAndRestoration() {
        runBlocking {
            val mangaName = "Chainsaw Man"

            // 1. Create auto cover
            val coversDir = File(context.filesDir, "covers")
            coversDir.mkdirs()
            val hash = CoverManager.hashKey(mangaName)
            val autoFile = File(coversDir, "auto_$hash.jpg")
            FileOutputStream(autoFile).use { it.write(byteArrayOf(1, 2, 3, 4, 5)) }

            val activeBefore = CoverManager.getCoverFile(context, mangaName)
            assertEquals("auto_$hash.jpg", activeBefore?.name)
            assertFalse(CoverManager.hasCustomCover(context, mangaName))

            // 2. Set custom cover
            val customSource = File(context.cacheDir, "custom_source.jpg")
            val testBmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            FileOutputStream(customSource).use { testBmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            testBmp.recycle()

            val customFile = CoverManager.setCustomCover(context, mangaName, Uri.fromFile(customSource))
            assertNotNull(customFile)
            assertTrue(CoverManager.hasCustomCover(context, mangaName))

            // Active cover must now be the custom cover
            val activeCustom = CoverManager.getCoverFile(context, mangaName)
            assertEquals("custom_$hash.jpg", activeCustom?.name)

            // 3. Reset custom cover
            val resetSuccess = CoverManager.resetCustomCover(context, mangaName)
            assertTrue(resetSuccess)
            assertFalse(CoverManager.hasCustomCover(context, mangaName))

            // Active cover must revert back to auto cover
            val activeAfterReset = CoverManager.getCoverFile(context, mangaName)
            assertEquals("auto_$hash.jpg", activeAfterReset?.name)

            customSource.delete()
            autoFile.delete()
        }
    }
}
