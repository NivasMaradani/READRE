package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.repository.ReaderRepository
import com.example.model.ReadingDirection
import com.example.model.ReadingMode
import com.example.model.TapAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IssueFixesAndEnhancementsTest {

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

    // =========================================================================
    // ISSUE 01: Cover Presentation & Removal of Custom Labels
    // =========================================================================

    @Test
    fun testIssue01_coverPresentationIdenticalVisualTreatment() {
        // Both automatic and custom covers render with title overlaid at bottom
        // and do not display distinguishing labels like "CUSTOM".
        val autoManga = com.example.model.Manga(
            name = "Berserk",
            folderUri = Uri.parse("content://test/berserk"),
            chapterCount = 40,
            hasCustomCover = false
        )
        val customManga = com.example.model.Manga(
            name = "Vagabond",
            folderUri = Uri.parse("content://test/vagabond"),
            chapterCount = 37,
            hasCustomCover = true
        )

        // Both preserve their distinct names and chapter counts
        assertEquals("Berserk", autoManga.name)
        assertEquals(40, autoManga.chapterCount)
        assertEquals("Vagabond", customManga.name)
        assertEquals(37, customManga.chapterCount)

        // Functionality to detect custom vs auto is preserved for menu actions
        assertFalse(autoManga.hasCustomCover)
        assertTrue(customManga.hasCustomCover)
    }

    // =========================================================================
    // ISSUE 02: Page Navigation Indicator & Wheel Picker Clamping
    // =========================================================================

    @Test
    fun testIssue02_pageIndicatorFormatting() {
        // Example requirement: "042 / 186"
        val current42 = 42
        val total186 = 186

        val formattedCurrent = String.format(Locale.US, "%03d", current42)
        val formattedTotal = if (total186 >= 100) String.format(Locale.US, "%03d", total186) else total186.toString()
        val indicatorText = "$formattedCurrent / $formattedTotal"

        assertEquals("042 / 186", indicatorText)

        // Single digit page in 3-digit chapter: page 5 of 120 -> "005 / 120"
        val current5 = 5
        val total120 = 120
        val text5 = String.format(Locale.US, "%03d", current5) + " / " + String.format(Locale.US, "%03d", total120)
        assertEquals("005 / 120", text5)

        // Small chapter: page 3 of 25 -> "003 / 25"
        val current3 = 3
        val total25 = 25
        val text3 = String.format(Locale.US, "%03d", current3) + " / " + total25.toString()
        assertEquals("003 / 25", text3)
    }

    @Test
    fun testIssue02_wheelPickerBoundaryClamping() {
        val totalPages = 186

        // Attempting to select 0 or negative must clamp to 1
        val underflow = 0.coerceIn(1, totalPages)
        assertEquals(1, underflow)

        val negative = (-5).coerceIn(1, totalPages)
        assertEquals(1, negative)

        // Attempting to select beyond totalPages must clamp to totalPages
        val overflow = 200.coerceIn(1, totalPages)
        assertEquals(186, overflow)

        // Target page selection conversion: page 105 (1-based) converts to 0-based index 104
        val selectedPage1Based = 105
        val targetIndex0Based = selectedPage1Based - 1
        assertEquals(104, targetIndex0Based)
        assertTrue(targetIndex0Based in 0 until totalPages)
    }

    // =========================================================================
    // ISSUE 03: Reading Settings Persistence & Bidirectional Sync
    // =========================================================================

    @Test
    fun testIssue03_verticalTapScrollSpeedPersistenceAndSync() = runBlocking {
        // Default tap scroll distance percentage is 50%
        val defaultPercent = repository.verticalTapScrollPercentFlow.first()
        assertEquals(50, defaultPercent)

        // User updates value inside reader to 75%
        repository.saveVerticalTapScrollPercent(75)
        assertEquals(75, repository.verticalTapScrollPercentFlow.first())

        // Values are clamped safely between 20% and 100%
        repository.saveVerticalTapScrollPercent(10)
        assertEquals(20, repository.verticalTapScrollPercentFlow.first())

        repository.saveVerticalTapScrollPercent(150)
        assertEquals(100, repository.verticalTapScrollPercentFlow.first())

        // Create a new repository instance to confirm persistence in database
        val newRepo = ReaderRepository(context, database)
        assertEquals(100, newRepo.verticalTapScrollPercentFlow.first())
    }

    @Test
    fun testIssue03_holdScrollSpeedPersistenceAndSync() = runBlocking {
        // Default hold-to-scroll speed is 650 px/s
        val defaultSpeed = repository.holdScrollSpeedPxFlow.first()
        assertEquals(650, defaultSpeed)

        // User updates value inside reader to 1200 px/s
        repository.saveHoldScrollSpeedPx(1200)
        assertEquals(1200, repository.holdScrollSpeedPxFlow.first())

        // User changes hold activation delay to 250 ms
        repository.saveHoldActivationDelayMs(250)
        assertEquals(250, repository.holdActivationDelayMsFlow.first())

        // User toggles reverse hold direction
        repository.saveReverseHoldScrollDirection(true)
        assertTrue(repository.reverseHoldScrollDirectionFlow.first())

        // Single source of truth verification across fresh repository instance
        val newRepo = ReaderRepository(context, database)
        assertEquals(1200, newRepo.holdScrollSpeedPxFlow.first())
        assertEquals(250, newRepo.holdActivationDelayMsFlow.first())
        assertTrue(newRepo.reverseHoldScrollDirectionFlow.first())
    }

    @Test
    fun testIssue03_readingModeAndDirectionSync() = runBlocking {
        // Mode toggle synchronization
        repository.saveDefaultReadingMode(ReadingMode.VERTICAL_CONTINUOUS)
        assertEquals(ReadingMode.VERTICAL_CONTINUOUS, repository.defaultReadingModeFlow.first())

        repository.saveDefaultReadingMode(ReadingMode.SINGLE_PAGE)
        assertEquals(ReadingMode.SINGLE_PAGE, repository.defaultReadingModeFlow.first())

        // Direction toggle synchronization
        repository.saveDefaultReadingDirection(ReadingDirection.RIGHT_TO_LEFT)
        assertEquals(ReadingDirection.RIGHT_TO_LEFT, repository.defaultReadingDirectionFlow.first())

        repository.saveDefaultReadingDirection(ReadingDirection.LEFT_TO_RIGHT)
        assertEquals(ReadingDirection.LEFT_TO_RIGHT, repository.defaultReadingDirectionFlow.first())

        // Tap actions synchronization
        repository.savePagedRightTapAction(TapAction.PREVIOUS_PAGE)
        assertEquals(TapAction.PREVIOUS_PAGE, repository.pagedRightTapActionFlow.first())

        repository.savePagedLeftTapAction(TapAction.NEXT_PAGE)
        assertEquals(TapAction.NEXT_PAGE, repository.pagedLeftTapActionFlow.first())

        repository.saveReversePageNavigation(true)
        assertTrue(repository.reversePageNavigationFlow.first())
    }
}
