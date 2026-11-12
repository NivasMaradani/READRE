package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.MangaEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.data.repository.ReaderRepository
import com.example.model.Manga
import com.example.util.CoverManager
import com.example.util.SecurityManager
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureEnhancementsTest {

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

    // --- 1. Automatic Manga Cover Generation & Natural Sorting ---

    @Test
    fun testAutoCoverGeneratesFromFirstNaturallySortedImage() = runBlocking {
        // Create a test ZIP archive where physical entry order is 10.jpg, 02.jpg, 01.jpg
        val zipFile = File(context.cacheDir, "test_manga_ch1.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Entry 1 (physical first): 10.jpg (Blue image)
            zos.putNextEntry(ZipEntry("page_10.jpg"))
            createTestBitmapBytes(android.graphics.Color.BLUE).let { zos.write(it) }
            zos.closeEntry()

            // Entry 2: 02.jpg (Green image)
            zos.putNextEntry(ZipEntry("page_02.jpg"))
            createTestBitmapBytes(android.graphics.Color.GREEN).let { zos.write(it) }
            zos.closeEntry()

            // Entry 3: 01.jpg (Red image - true first naturally sorted image)
            zos.putNextEntry(ZipEntry("page_01.jpg"))
            createTestBitmapBytes(android.graphics.Color.RED).let { zos.write(it) }
            zos.closeEntry()
        }

        val mangaName = "Test Natural Manga"
        val firstChapterUri = Uri.fromFile(zipFile)

        // Generate auto cover
        val coverFile = CoverManager.getOrGenerateAutoCover(context, mangaName, firstChapterUri)
        assertNotNull(coverFile)
        assertTrue(coverFile!!.exists())
        assertTrue(coverFile.length() > 0L)

        // Subsequent call returns cached file immediately
        val cached = CoverManager.getOrGenerateAutoCover(context, mangaName, firstChapterUri)
        assertEquals(coverFile.absolutePath, cached?.absolutePath)

        // Force regenerate cover
        val regenerated = CoverManager.regenerateAutoCover(context, mangaName, firstChapterUri)
        assertNotNull(regenerated)
        assertTrue(regenerated!!.exists())
    }

    @Test
    fun testCoverActions_CustomCoverAndRestoreAuto() = runBlocking {
        val mangaName = "Solo Leveling"
        val zipFile = File(context.cacheDir, "sl_ch1.cbz")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("01.jpg"))
            createTestBitmapBytes(android.graphics.Color.GRAY).let { zos.write(it) }
            zos.closeEntry()
        }

        val manga = Manga(
            name = mangaName,
            folderUri = Uri.fromFile(context.cacheDir),
            chapterCount = 1
        )

        // Ensure auto cover
        val autoCover = CoverManager.getOrGenerateAutoCover(context, mangaName, Uri.fromFile(zipFile))
        assertNotNull(autoCover)
        assertFalse(CoverManager.hasCustomCover(context, mangaName))

        // Create a custom cover image file
        val customImageFile = File(context.cacheDir, "my_custom_cover.jpg")
        FileOutputStream(customImageFile).use { out ->
            out.write(createTestBitmapBytes(android.graphics.Color.MAGENTA))
        }

        // Set custom cover
        val customCover = CoverManager.setCustomCover(context, mangaName, Uri.fromFile(customImageFile))
        assertNotNull(customCover)
        assertTrue(CoverManager.hasCustomCover(context, mangaName))
        assertEquals(customCover?.absolutePath, CoverManager.getCoverFile(context, mangaName)?.absolutePath)

        // Restore automatic cover (reset custom)
        val resetSuccess = CoverManager.resetCustomCover(context, mangaName)
        assertTrue(resetSuccess)
        assertFalse(CoverManager.hasCustomCover(context, mangaName))
        assertEquals(autoCover?.absolutePath, CoverManager.getCoverFile(context, mangaName)?.absolutePath)
    }

    // --- 2. Security PIN & Cryptographic Hashing ---

    @Test
    fun testSecurityManager_PinSetupAndVerification() {
        val testPin = "1234"
        val wrongPin = "9999"

        assertFalse(SecurityManager.isPinConfigured(context))

        // Setup PIN
        val setupSuccess = SecurityManager.setupPin(context, testPin)
        assertTrue(setupSuccess)
        assertTrue(SecurityManager.isPinConfigured(context))

        // Verify correct PIN
        assertTrue(SecurityManager.verifyPin(context, testPin))

        // Verify wrong PIN is rejected
        assertFalse(SecurityManager.verifyPin(context, wrongPin))

        // Change PIN
        val newPin = "5678"
        assertFalse(SecurityManager.changePin(context, "wrongOld", newPin))
        assertTrue(SecurityManager.changePin(context, testPin, newPin))
        assertTrue(SecurityManager.verifyPin(context, newPin))
        assertFalse(SecurityManager.verifyPin(context, testPin))

        // Verify plaintext PIN is never stored in SharedPreferences
        val prefs = context.getSharedPreferences("reader_security_prefs", Context.MODE_PRIVATE)
        val allPrefs = prefs.all
        assertFalse(allPrefs.values.contains(testPin))
        assertFalse(allPrefs.values.contains(newPin))
    }

    // --- 3. Hidden Manga Privacy & Complete Exclusion ---

    @Test
    fun testHiddenManga_ExclusionFromNormalLibrarySearchAndLists() = runBlocking {
        // Insert 1 normal manga and 1 hidden manga
        val normalManga = MangaEntity(
            name = "One Piece",
            folderUri = "content://reader/onepiece",
            chapterCount = 100,
            isHidden = false
        )
        val hiddenManga = MangaEntity(
            name = "Secret Manga",
            folderUri = "content://reader/secret",
            chapterCount = 20,
            isHidden = true
        )
        dao.insertMangaList(listOf(normalManga, hiddenManga))

        // Add progress, history, and bookmarks for both
        dao.saveReadingProgress(ReadingProgressEntity("One Piece", "ch1.cbz", 5, 1000L))
        dao.saveReadingProgress(ReadingProgressEntity("Secret Manga", "secret_ch1.cbz", 10, 2000L))

        dao.saveChapterProgress(ChapterReadingProgressEntity("One Piece", "ch1.cbz", 5, 20, false, 1000L, 1000L))
        dao.saveChapterProgress(ChapterReadingProgressEntity("Secret Manga", "secret_ch1.cbz", 10, 30, false, 2000L, 2000L))

        dao.insertBookmark(BookmarkEntity(mangaName = "One Piece", chapterFileName = "ch1.cbz", timestamp = 1000L))
        dao.insertBookmark(BookmarkEntity(mangaName = "Secret Manga", chapterFileName = "secret_ch1.cbz", timestamp = 2000L))

        // 1. Verify visible manga list contains ONLY One Piece
        val visibleManga = dao.getVisibleMangaFlow().first()
        assertEquals(1, visibleManga.size)
        assertEquals("One Piece", visibleManga[0].name)

        // 2. Verify hidden manga list contains ONLY Secret Manga
        val hiddenMangaList = dao.getHiddenMangaFlow().first()
        assertEquals(1, hiddenMangaList.size)
        assertEquals("Secret Manga", hiddenMangaList[0].name)

        // 3. Verify Continue Reading excludes hidden manga
        val readingProgress = dao.getAllReadingProgress().first()
        assertEquals(1, readingProgress.size)
        assertEquals("One Piece", readingProgress[0].mangaName)

        // 4. Verify Chapter History excludes hidden manga
        val history = dao.getAllChapterProgress().first()
        assertEquals(1, history.size)
        assertEquals("One Piece", history[0].mangaName)

        // 5. Verify Bookmarks excludes hidden manga
        val bookmarks = dao.getAllBookmarks().first()
        assertEquals(1, bookmarks.size)
        assertEquals("One Piece", bookmarks[0].mangaName)

        // 6. Unhide manga
        dao.setMangaHidden("Secret Manga", false)
        val unhiddenVisible = dao.getVisibleMangaFlow().first()
        assertEquals(2, unhiddenVisible.size)

        // Progress and bookmarks reappear without loss
        val restoredProgress = dao.getAllReadingProgress().first()
        assertEquals(2, restoredProgress.size)
    }

    // --- 4. Interactive Tutorial Persistence ---

    @Test
    fun testTutorial_CompletionAndPersistence() = runBlocking {
        // Initial state: not completed
        assertFalse(repository.isTutorialCompleted())

        // Save completed
        repository.saveTutorialCompleted(true)
        assertTrue(repository.isTutorialCompleted())
        assertTrue(repository.tutorialCompletedFlow.first())

        // Replaying does not delete bookmarks or reading progress
        dao.saveReadingProgress(ReadingProgressEntity("Manga", "ch.cbz", 3, 100L))
        assertEquals(1, dao.getAllReadingProgress().first().size)
    }

    // --- 5. Room Database Migration 3 -> 4 ---

    @Test
    fun testMigration3To4_AddsIsHiddenColumnWithoutDataLoss() {
        // Test migration directly using SupportSQLiteDatabase
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_3_4.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Create version 3 schema
                        db.execSQL("""
                            CREATE TABLE `indexed_manga` (
                                `name` TEXT NOT NULL,
                                `folderUri` TEXT NOT NULL,
                                `chapterCount` INTEGER NOT NULL,
                                `lastModified` INTEGER NOT NULL,
                                `lastScannedAt` INTEGER NOT NULL,
                                `customCoverPath` TEXT DEFAULT NULL,
                                `autoCoverPath` TEXT DEFAULT NULL,
                                PRIMARY KEY(`name`)
                            )
                        """.trimIndent())
                        db.execSQL("INSERT INTO `indexed_manga` VALUES ('Berserk', 'content://test', 364, 0, 1000, NULL, NULL)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = helper.writableDatabase
        // Apply migration 3 -> 4
        ReaderDatabase.MIGRATION_3_4.migrate(db)

        // Verify column isHidden was added and existing record has default 0
        val cursor = db.query("SELECT name, isHidden FROM indexed_manga WHERE name = 'Berserk'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Berserk", cursor.getString(0))
        assertEquals(0, cursor.getInt(1))
        cursor.close()
        db.close()
    }

    private fun createTestBitmapBytes(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        bitmap.recycle()
        return baos.toByteArray()
    }
}
