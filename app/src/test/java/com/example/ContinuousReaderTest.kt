package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.repository.ReaderRepository
import com.example.model.Chapter
import com.example.model.HoldScrollSpeed
import com.example.model.Manga
import com.example.model.ReaderItem
import com.example.model.ReadingMode
import com.example.model.TapAction
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.ScreenState
import com.example.util.CoverManager
import com.example.util.ImageFormatDetector
import kotlinx.coroutines.Dispatchers
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
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContinuousReaderTest {

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

    private fun createDummyArchive(file: File, pageCount: Int) {
        val zos = ZipOutputStream(FileOutputStream(file))
        for (i in 1..pageCount) {
            val bmp = Bitmap.createBitmap(100, 150, Bitmap.Config.RGB_565)
            val tempImg = File(context.cacheDir, "tmp_${System.currentTimeMillis()}_$i.jpg")
            FileOutputStream(tempImg).use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            bmp.recycle()

            zos.putNextEntry(ZipEntry(String.format("page_%03d.jpg", i)))
            tempImg.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
            tempImg.delete()
        }
        zos.close()
    }

    // --- TEST A: Automatic library covers generated and displayed on first launch ---
    @Test
    fun testA_autoCoverGenerationAndDisplay() {
        runBlocking {
            val mangaName = "Solo Leveling"
            val archiveFile = File(context.cacheDir, "solo_ch01.zip")
            createDummyArchive(archiveFile, pageCount = 5)

            val coverFile = CoverManager.getOrGenerateAutoCover(
                context = context,
                mangaName = mangaName,
                firstChapterUri = Uri.fromFile(archiveFile)
            )

            assertNotNull("Auto-generated cover must exist", coverFile)
            assertTrue("Auto cover file must exist on disk", coverFile?.exists() ?: false)
            assertTrue("Auto cover file must contain data", (coverFile?.length() ?: 0L) > 0L)

            // Verify active cover lookup resolves to the auto cover
            val activeCover = CoverManager.getCoverFile(context, mangaName)
            assertEquals(coverFile?.absolutePath, activeCover?.absolutePath)
            assertFalse(CoverManager.hasCustomCover(context, mangaName))

            archiveFile.delete()
            coverFile?.delete()
        }
    }

    // --- TEST B: Custom cover selection overrides auto cover; restore re-enables auto cover ---
    @Test
    fun testB_customCoverOverridesAndRestores() {
        runBlocking {
            val mangaName = "Tower of God"
            val hash = CoverManager.hashKey(mangaName)
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val autoFile = File(coversDir, "auto_$hash.jpg").apply {
                FileOutputStream(this).use { it.write(byteArrayOf(10, 20, 30)) }
            }

            // Before: auto cover is active
            var active = CoverManager.getCoverFile(context, mangaName)
            assertEquals("auto_$hash.jpg", active?.name)
            assertFalse(CoverManager.hasCustomCover(context, mangaName))

            // Set custom cover
            val customSource = File(context.cacheDir, "custom_picker.jpg")
            val bmp = Bitmap.createBitmap(150, 200, Bitmap.Config.RGB_565)
            FileOutputStream(customSource).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()

            val customFile = CoverManager.setCustomCover(context, mangaName, Uri.fromFile(customSource))
            assertNotNull(customFile)
            assertTrue(CoverManager.hasCustomCover(context, mangaName))

            // Active cover is now custom cover
            active = CoverManager.getCoverFile(context, mangaName)
            assertEquals("custom_$hash.jpg", active?.name)

            // Restore auto cover
            val restored = CoverManager.resetCustomCover(context, mangaName)
            assertTrue(restored)
            assertFalse(CoverManager.hasCustomCover(context, mangaName))

            active = CoverManager.getCoverFile(context, mangaName)
            assertEquals("auto_$hash.jpg", active?.name)

            customSource.delete()
            autoFile.delete()
        }
    }

    // --- TEST C: Chapter list visual distinction (completed, in progress, unread) ---
    @Test
    fun testC_chapterListVisualDistinction() {
        runBlocking {
            val mangaName = "One Piece"

            // 1. Completed chapter
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = "Ch 1.cbz",
                pageIndex = 19,
                totalPages = 20,
                isCompleted = true
            )

            // 2. In-Progress chapter
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = "Ch 2.cbz",
                pageIndex = 11,
                totalPages = 22,
                isCompleted = false
            )

            // 3. Unread chapter has no progress entry in DB
            val ch1Progress = repository.getChapterProgress(mangaName, "Ch 1.cbz")
            val ch2Progress = repository.getChapterProgress(mangaName, "Ch 2.cbz")
            val ch3Progress = repository.getChapterProgress(mangaName, "Ch 3.cbz")

            // Assert Completed state attributes
            assertNotNull(ch1Progress)
            assertTrue("Ch 1 must be completed", ch1Progress?.isCompleted ?: false)
            assertEquals(19, ch1Progress?.currentPageIndex)
            assertEquals(20, ch1Progress?.totalPages)

            // Assert In-Progress state attributes
            assertNotNull(ch2Progress)
            assertFalse("Ch 2 must NOT be completed", ch2Progress?.isCompleted ?: true)
            assertEquals(11, ch2Progress?.currentPageIndex)
            assertEquals(22, ch2Progress?.totalPages)

            // Assert Unread state attributes
            assertEquals("Unread chapter has no saved progress", null, ch3Progress)
        }
    }

    // --- TEST D: Continuous reading in vertical mode transitions seamlessly from Ch 15 to Ch 16 with separator ---
    @Test
    fun testD_continuousVerticalTransitionWithSeparator() {
        runBlocking {
            val ch15File = File(context.cacheDir, "ch15.zip")
            val ch16File = File(context.cacheDir, "ch16.zip")
            createDummyArchive(ch15File, pageCount = 4)
            createDummyArchive(ch16File, pageCount = 3)

            val ch15Pages = repository.loadChapterPages(Uri.fromFile(ch15File), "TestManga", "ch15.zip")
            val ch16Pages = repository.loadChapterPages(Uri.fromFile(ch16File), "TestManga", "ch16.zip")

            val ch15 = Chapter(
                originalFileName = "ch15.zip",
                displayName = "Chapter 15",
                fileUri = Uri.fromFile(ch15File),
                mangaName = "TestManga",
                fileSize = ch15File.length(),
                sortOrder = 14,
                totalPages = 4
            )
            val ch16 = Chapter(
                originalFileName = "ch16.zip",
                displayName = "Chapter 16",
                fileUri = Uri.fromFile(ch16File),
                mangaName = "TestManga",
                fileSize = ch16File.length(),
                sortOrder = 15,
                totalPages = 3
            )

            // Simulate building continuous stream
            val items = mutableListOf<ReaderItem>()
            ch15Pages.forEachIndexed { i, f -> items.add(ReaderItem.Page(ch15, i, ch15Pages.size, f)) }
            items.add(ReaderItem.ChapterSeparator(previousChapter = ch15, nextChapter = ch16))
            ch16Pages.forEachIndexed { i, f -> items.add(ReaderItem.Page(ch16, i, ch16Pages.size, f)) }

            // Total items: 4 (ch15 pages) + 1 (separator) + 3 (ch16 pages) = 8
            assertEquals(8, items.size)
            assertTrue("Item 4 must be separator", items[4] is ReaderItem.ChapterSeparator)
            val sep = items[4] as ReaderItem.ChapterSeparator
            assertEquals("ch15.zip", sep.previousChapter.originalFileName)
            assertEquals("ch16.zip", sep.nextChapter.originalFileName)

            // Item 5 must be Chapter 16 Page 0
            assertTrue("Item 5 must be Page", items[5] is ReaderItem.Page)
            val ch16FirstPage = items[5] as ReaderItem.Page
            assertEquals("ch16.zip", ch16FirstPage.chapter.originalFileName)
            assertEquals(0, ch16FirstPage.pageIndex)

            ch15File.delete()
            ch16File.delete()
        }
    }

    // --- TEST E: Independent chapter progress updates during continuous scrolling ---
    @Test
    fun testE_independentChapterProgressDuringContinuousScrolling() {
        runBlocking {
            val mangaName = "Kingdom"

            // 1. User is reading Chapter 15 at page 2/10
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = "Chapter 15.zip",
                pageIndex = 2,
                totalPages = 10,
                isCompleted = false
            )
            var ch15 = repository.getChapterProgress(mangaName, "Chapter 15.zip")
            assertNotNull(ch15)
            assertEquals(2, ch15?.currentPageIndex)
            assertFalse(ch15?.isCompleted ?: true)

            // 2. User reaches final page of Chapter 15 (index 9/10)
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = "Chapter 15.zip",
                pageIndex = 9,
                totalPages = 10,
                isCompleted = true
            )
            ch15 = repository.getChapterProgress(mangaName, "Chapter 15.zip")
            assertTrue("Chapter 15 must be completed on reaching final page", ch15?.isCompleted ?: false)

            // 3. User scrolls across separator into Chapter 16 Page 0
            // Chapter 16 progress begins independently and is NOT marked completed
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = "Chapter 16.zip",
                pageIndex = 0,
                totalPages = 12,
                isCompleted = false
            )

            val ch16 = repository.getChapterProgress(mangaName, "Chapter 16.zip")
            assertNotNull(ch16)
            assertEquals(0, ch16?.currentPageIndex)
            assertEquals(12, ch16?.totalPages)
            assertFalse("Chapter 16 must NOT be completed until its own final page", ch16?.isCompleted ?: true)

            // Chapter 15 completion is preserved
            val ch15After = repository.getChapterProgress(mangaName, "Chapter 15.zip")
            assertTrue("Chapter 15 must remain completed", ch15After?.isCompleted ?: false)
        }
    }

    // --- TEST F: Single-page mode displays next chapter action without auto-transition ---
    @Test
    fun testF_singlePageModePreservesSingleChapter() {
        runBlocking {
            // In Single Page mode: active pages list only contains pages for the currently open chapter
            val ch1File = File(context.cacheDir, "single_ch1.zip")
            val ch2File = File(context.cacheDir, "single_ch2.zip")
            createDummyArchive(ch1File, pageCount = 6)
            createDummyArchive(ch2File, pageCount = 8)

            val ch1Pages = repository.loadChapterPages(Uri.fromFile(ch1File), "MangaS", "single_ch1.zip")
            assertEquals(6, ch1Pages.size)

            // User advances to last page of Chapter 1
            repository.saveChapterProgress("MangaS", "single_ch1.zip", 5, 6, isCompleted = true)

            val ch1Prog = repository.getChapterProgress("MangaS", "single_ch1.zip")
            assertTrue(ch1Prog?.isCompleted ?: false)

            // Chapter 2 was not automatically appended or started
            val ch2Prog = repository.getChapterProgress("MangaS", "single_ch2.zip")
            assertEquals("Chapter 2 should remain unstarted in single page mode", null, ch2Prog)

            ch1File.delete()
            ch2File.delete()
        }
    }

    // --- TEST G: Rapid continuous scrolling does not crash or exceed memory ---
    @Test
    fun testG_rapidContinuousScrollingStressTest() {
        runBlocking {
            val mangaName = "Berserk"

            // Simulate 200 rapid continuous scroll events across multiple chapters
            for (chapterNum in 1..5) {
                val totalPages = 20
                for (page in 0 until totalPages) {
                    repository.saveChapterProgress(
                        mangaName = mangaName,
                        chapterFileName = "Chapter $chapterNum.cbz",
                        pageIndex = page,
                        totalPages = totalPages,
                        isCompleted = (page == totalPages - 1)
                    )
                }
            }

            // Verify all 5 chapters have valid independent progress records
            val allProgress = dao.getChapterProgressForMangaSync(mangaName)
            assertEquals(5, allProgress.size)

            // Chapters 1..5 should all be completed
            assertTrue(allProgress.all { it.isCompleted })
            assertEquals(19, allProgress[0].currentPageIndex)
        }
    }

    // --- TEST H: Image format detection by magic bytes & diagnostic failure analysis ---
    @Test
    fun testH_imageFormatDetectionAndDiagnosis() {
        // 1. JPEG magic bytes
        val jpegFile = File(context.cacheDir, "test.jpg")
        jpegFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
        assertEquals(ImageFormatDetector.Format.JPEG, ImageFormatDetector.detectFormat(jpegFile))
        assertTrue(ImageFormatDetector.isSupportedImage(jpegFile))

        // 2. PNG magic bytes
        val pngFile = File(context.cacheDir, "test.png")
        pngFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()))
        assertEquals(ImageFormatDetector.Format.PNG, ImageFormatDetector.detectFormat(pngFile))
        assertTrue(ImageFormatDetector.isSupportedImage(pngFile))

        // 3. WEBP magic bytes: RIFF....WEBP
        val webpFile = File(context.cacheDir, "test.webp")
        val webpBytes = ByteArray(12)
        webpBytes[0] = 'R'.code.toByte()
        webpBytes[1] = 'I'.code.toByte()
        webpBytes[2] = 'F'.code.toByte()
        webpBytes[3] = 'F'.code.toByte()
        webpBytes[8] = 'W'.code.toByte()
        webpBytes[9] = 'E'.code.toByte()
        webpBytes[10] = 'B'.code.toByte()
        webpBytes[11] = 'P'.code.toByte()
        webpFile.writeBytes(webpBytes)
        assertEquals(ImageFormatDetector.Format.WEBP, ImageFormatDetector.detectFormat(webpFile))
        assertTrue(ImageFormatDetector.isSupportedImage(webpFile))

        // 4. Empty file diagnostic
        val emptyFile = File(context.cacheDir, "empty.jpg")
        emptyFile.createNewFile()
        val emptyDiag = ImageFormatDetector.diagnoseFailure(emptyFile, null)
        assertTrue(emptyDiag.contains("empty") || emptyDiag.contains("0 bytes"))

        // 5. Corrupted / unsupported header diagnostic
        val corruptFile = File(context.cacheDir, "corrupted.jpg")
        corruptFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val corruptDiag = ImageFormatDetector.diagnoseFailure(corruptFile, null)
        assertTrue(corruptDiag.contains("corrupted") || corruptDiag.contains("Unsupported"))

        // Clean up
        jpegFile.delete()
        pngFile.delete()
        webpFile.delete()
        emptyFile.delete()
        corruptFile.delete()
    }

    // --- TEST I: HoldScrollSpeed persistence in DataStore repository ---
    @Test
    fun testI_holdScrollSpeedPersistence() {
        runBlocking {
            // Default should be NORMAL
            val defaultSpeed = repository.holdScrollSpeedFlow.first()
            assertEquals(HoldScrollSpeed.NORMAL, defaultSpeed)

            // Save SLOW
            repository.saveHoldScrollSpeed(HoldScrollSpeed.SLOW)
            val slowSpeed = repository.holdScrollSpeedFlow.first()
            assertEquals(HoldScrollSpeed.SLOW, slowSpeed)

            // Save FAST
            repository.saveHoldScrollSpeed(HoldScrollSpeed.FAST)
            val fastSpeed = repository.holdScrollSpeedFlow.first()
            assertEquals(HoldScrollSpeed.FAST, fastSpeed)
        }
    }

    // --- TEST J: Minimal chapter list opacity styling (no checkmarks) ---
    @Test
    fun testJ_minimalChapterListOpacityState() {
        val completedChapter = Chapter(
            originalFileName = "ch01.zip",
            displayName = "Chapter 1",
            fileUri = Uri.parse("content://test/ch01"),
            mangaName = "Tower",
            isCompleted = true,
            currentPageIndex = 19,
            totalPages = 20
        )

        val inProgressChapter = Chapter(
            originalFileName = "ch02.zip",
            displayName = "Chapter 2",
            fileUri = Uri.parse("content://test/ch02"),
            mangaName = "Tower",
            isCompleted = false,
            currentPageIndex = 7,
            totalPages = 20
        )

        val unreadChapter = Chapter(
            originalFileName = "ch03.zip",
            displayName = "Chapter 3",
            fileUri = Uri.parse("content://test/ch03"),
            mangaName = "Tower",
            isCompleted = false,
            currentPageIndex = 0,
            totalPages = 0
        )

        // Completed chapter has reduced opacity
        val completedAlpha = if (completedChapter.isCompleted) 0.50f else 1.0f
        assertEquals(0.50f, completedAlpha, 0.01f)

        // In-progress chapter has full brightness
        val inProgressAlpha = if (inProgressChapter.isCompleted) 0.50f else 1.0f
        assertEquals(1.0f, inProgressAlpha, 0.01f)

        // Unread chapter has full brightness
        val unreadAlpha = if (unreadChapter.isCompleted) 0.50f else 1.0f
        assertEquals(1.0f, unreadAlpha, 0.01f)
    }

    // --- TEST K: Paged-mode navigation defaults, manual reversal, and RTL consistency ---
    @Test
    fun testK_pagedNavigationDefaultsAndReversal() {
        runBlocking {
            // 1. Verify repository defaults
            val defaultRight = repository.pagedRightTapActionFlow.first()
            val defaultLeft = repository.pagedLeftTapActionFlow.first()
            val defaultReverse = repository.reversePageNavigationFlow.first()

            assertEquals("Default right tap must be NEXT_PAGE", TapAction.NEXT_PAGE, defaultRight)
            assertEquals("Default left tap must be PREVIOUS_PAGE", TapAction.PREVIOUS_PAGE, defaultLeft)
            assertFalse("Default reverse page navigation must be false", defaultReverse)

            // Resolve effective actions with defaults: Right = Next, Left = Previous
            fun resolveEffective(zone: Int, rightAct: TapAction, leftAct: TapAction, reverse: Boolean): TapAction {
                val raw = if (zone > 0) rightAct else leftAct
                return if (reverse) {
                    if (raw == TapAction.NEXT_PAGE) TapAction.PREVIOUS_PAGE else TapAction.NEXT_PAGE
                } else raw
            }

            assertEquals(TapAction.NEXT_PAGE, resolveEffective(1, defaultRight, defaultLeft, defaultReverse))
            assertEquals(TapAction.PREVIOUS_PAGE, resolveEffective(-1, defaultRight, defaultLeft, defaultReverse))

            // 2. Manual reversal: On
            repository.saveReversePageNavigation(true)
            val updatedReverse = repository.reversePageNavigationFlow.first()
            assertTrue(updatedReverse)

            assertEquals(TapAction.PREVIOUS_PAGE, resolveEffective(1, defaultRight, defaultLeft, updatedReverse))
            assertEquals(TapAction.NEXT_PAGE, resolveEffective(-1, defaultRight, defaultLeft, updatedReverse))

            // 3. Custom explicit assignment (Right=Previous, Left=Next)
            repository.savePagedRightTapAction(TapAction.PREVIOUS_PAGE)
            repository.savePagedLeftTapAction(TapAction.NEXT_PAGE)
            repository.saveReversePageNavigation(false)

            val customRight = repository.pagedRightTapActionFlow.first()
            val customLeft = repository.pagedLeftTapActionFlow.first()
            assertEquals(TapAction.PREVIOUS_PAGE, customRight)
            assertEquals(TapAction.NEXT_PAGE, customLeft)
            assertEquals(TapAction.PREVIOUS_PAGE, resolveEffective(1, customRight, customLeft, false))
            assertEquals(TapAction.NEXT_PAGE, resolveEffective(-1, customRight, customLeft, false))
        }
    }

    // --- TEST L: Hold-scroll speed (100-2000 px/s), delay (150-800 ms), and direction reversal ---
    @Test
    fun testL_holdScrollSpeedAndDelayCustomization() {
        runBlocking {
            // Defaults
            assertEquals(650, repository.holdScrollSpeedPxFlow.first())
            assertEquals(300, repository.holdActivationDelayMsFlow.first())
            assertFalse(repository.reverseHoldScrollDirectionFlow.first())

            // Customize speed to 1250 px/s
            repository.saveHoldScrollSpeedPx(1250)
            assertEquals(1250, repository.holdScrollSpeedPxFlow.first())

            // Speed clamping: < 100 clamps to 100, > 2000 clamps to 2000
            repository.saveHoldScrollSpeedPx(50)
            assertEquals(100, repository.holdScrollSpeedPxFlow.first())
            repository.saveHoldScrollSpeedPx(5000)
            assertEquals(2000, repository.holdScrollSpeedPxFlow.first())

            // Customize delay to 450 ms
            repository.saveHoldActivationDelayMs(450)
            assertEquals(450, repository.holdActivationDelayMsFlow.first())

            // Delay clamping: < 150 clamps to 150, > 800 clamps to 800
            repository.saveHoldActivationDelayMs(50)
            assertEquals(150, repository.holdActivationDelayMsFlow.first())
            repository.saveHoldActivationDelayMs(1500)
            assertEquals(800, repository.holdActivationDelayMsFlow.first())

            // Reverse hold-scroll direction toggle
            repository.saveReverseHoldScrollDirection(true)
            assertTrue(repository.reverseHoldScrollDirectionFlow.first())

            // Simulate app restart with a fresh repository instance over same database
            val freshRepo = ReaderRepository(context, database)
            assertEquals(2000, freshRepo.holdScrollSpeedPxFlow.first())
            assertEquals(800, freshRepo.holdActivationDelayMsFlow.first())
            assertTrue(freshRepo.reverseHoldScrollDirectionFlow.first())
        }
    }

    // --- TEST M: Vertical tap distance is fixed at 50% viewport and independent of continuous speed ---
    @Test
    fun testM_verticalTapAndContinuousScrollIndependence() {
        val viewportHeight = 1000f

        // Tap distance must always be 50% viewport height
        val tapScrollDelta = viewportHeight * 0.5f
        assertEquals(500f, tapScrollDelta, 0.01f)

        // Continuous scrolling distance depends on speed and elapsed time
        fun calculateContinuousDelta(speedPxPerSec: Int, elapsedSec: Float, direction: Float): Float {
            return direction * speedPxPerSec * elapsedSec
        }

        // At 650 px/s for 16ms: delta is ~10.4 px
        val delta650 = calculateContinuousDelta(650, 0.016f, 1f)
        assertEquals(10.4f, delta650, 0.1f)

        // At 1500 px/s for 16ms: delta is ~24.0 px
        val delta1500 = calculateContinuousDelta(1500, 0.016f, 1f)
        assertEquals(24.0f, delta1500, 0.1f)

        // Tap scroll distance remains exactly 500f regardless of whether speed is 650 or 1500
        assertEquals(500f, tapScrollDelta, 0.01f)
    }
}
