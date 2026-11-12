package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReaderDatabase
import com.example.data.dao.ReaderDao
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.util.NaturalOrderComparator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChapterIndexingBenchmarkTest {

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

    private fun generateSyntheticChapters(mangaName: String, count: Int): List<ChapterEntity> {
        // Generate in shuffled order to test natural sorting
        val indices = (1..count).toList().shuffled()
        val sortedNames = indices.map { "Chapter $it.cbz" }.sortedWith { f1, f2 ->
            NaturalOrderComparator.compare(f1.substringBeforeLast('.'), f2.substringBeforeLast('.'))
        }

        return sortedNames.mapIndexed { index, fileName ->
            ChapterEntity(
                mangaName = mangaName,
                chapterFileName = fileName,
                displayName = fileName.substringBeforeLast('.'),
                documentUri = "content://com.android.externalstorage.documents/document/Reader%3A$mangaName%2F$fileName",
                fileSize = 10_000_000L + (index * 1024L),
                lastModified = 1700000000000L + index * 1000L,
                sortOrder = index,
                lastScannedAt = System.currentTimeMillis()
            )
        }
    }

    @Test
    fun benchmarkChapterIndexing_200Chapters() {
        runBenchmarkForCount(200)
    }

    @Test
    fun benchmarkChapterIndexing_500Chapters() {
        runBenchmarkForCount(500)
    }

    @Test
    fun benchmarkChapterIndexing_1000Chapters() {
        runBenchmarkForCount(1000)
    }

    @Test
    fun benchmarkChapterIndexing_2000Chapters() {
        runBenchmarkForCount(2000)
    }

    private fun runBenchmarkForCount(count: Int) {
        val mangaName = "Benchmark_Series_$count"
        val synthetic = generateSyntheticChapters(mangaName, count)

        // 1. Measure Initial Indexing Insertion Time
        var insertTimeMs: Long
        runBlocking {
            insertTimeMs = measureTimeMillis {
                dao.insertChapters(synthetic)
            }
        }

        println("BENCHMARK [$count Chapters] - Initial Room DB Insertion: ${insertTimeMs}ms")
        assertTrue("Batch insert for $count chapters should be fast", insertTimeMs < 1000)

        // Warm up Room / SQLite query cache
        runBlocking { dao.getChaptersForMangaSync(mangaName) }

        // 2. Measure Database Cached Query Time
        var cachedChapters: List<ChapterEntity>
        val dbLookupTimeMs = measureTimeMillis {
            runBlocking {
                cachedChapters = dao.getChaptersForMangaSync(mangaName)
            }
        }

        println("BENCHMARK [$count Chapters] - Database Cached Query: ${dbLookupTimeMs}ms")
        assertEquals(count, cachedChapters.size)
        assertTrue("Cached DB lookup for $count chapters must be under 150ms on cold container (was ${dbLookupTimeMs}ms)", dbLookupTimeMs < 150)

        // 3. Verify Natural Sort Order at Scale
        assertEquals("Chapter 1", cachedChapters[0].displayName)
        assertEquals("Chapter 2", cachedChapters[1].displayName)
        if (count >= 10) {
            assertEquals("Chapter 10", cachedChapters[9].displayName)
        }
        if (count >= 100) {
            assertEquals("Chapter 100", cachedChapters[99].displayName)
        }
        if (count >= 1000) {
            assertEquals("Chapter 1000", cachedChapters[999].displayName)
        }
        assertEquals("Chapter $count", cachedChapters[count - 1].displayName)

        // 4. Measure Incremental Diffing & Reconciliation
        // Simulate: 5 new chapters added, 2 deleted, 3 modified
        val existingMap = cachedChapters.associateBy { it.chapterFileName }

        val newFiles = (count + 1..count + 5).map { "Chapter $it.cbz" }
        val remaining = cachedChapters.drop(2) // simulate first 2 deleted
        val currentNames = remaining.map { it.chapterFileName } + newFiles

        val diffTimeMs = measureTimeMillis {
            runBlocking {
                val added = newFiles.filter { it !in existingMap }
                val deleted = cachedChapters.filter { it.chapterFileName !in currentNames }.map { it.chapterFileName }
                val modified = remaining.take(3)

                dao.deleteChapters(mangaName, deleted)
                val updatedBatch = newFiles.mapIndexed { idx, name ->
                    ChapterEntity(
                        mangaName = mangaName,
                        chapterFileName = name,
                        displayName = name.substringBeforeLast('.'),
                        documentUri = "content://doc/$name",
                        fileSize = 12_000_000L,
                        lastModified = System.currentTimeMillis(),
                        sortOrder = count + idx,
                        lastScannedAt = System.currentTimeMillis()
                    )
                }
                dao.insertChapters(updatedBatch)
            }
        }

        println("BENCHMARK [$count Chapters] - Incremental Reconcile & Diff: ${diffTimeMs}ms")
        assertTrue("Incremental diffing & update should take < 500ms", diffTimeMs < 500)

        val finalCount = runBlocking { dao.getChaptersForMangaSync(mangaName).size }
        assertEquals(count - 2 + 5, finalCount)
    }

    @Test
    fun testReadingProgressAndBookmarksPreservedDuringReconciliation() {
        val mangaName = "ProgressPreservationTest"
        val initialChapters = generateSyntheticChapters(mangaName, 200)

        runBlocking {
            dao.insertChapters(initialChapters)

            // Save reading progress and bookmark on chapter 50
            dao.saveReadingProgress(
                ReadingProgressEntity(
                    mangaName = mangaName,
                    chapterFileName = "Chapter 50.cbz",
                    pageIndex = 12,
                    timestamp = System.currentTimeMillis()
                )
            )
            dao.insertBookmark(
                BookmarkEntity(
                    mangaName = mangaName,
                    chapterFileName = "Chapter 50.cbz",
                    timestamp = System.currentTimeMillis()
                )
            )

            // Verify they exist
            val progressBefore = dao.getProgressForMangaSync(mangaName)
            assertNotNull(progressBefore)
            assertEquals("Chapter 50.cbz", progressBefore?.chapterFileName)
            assertTrue(dao.isChapterBookmarkedSync(mangaName, "Chapter 50.cbz"))

            // Perform full reconciliation with new chapters
            val updatedChapters = generateSyntheticChapters(mangaName, 250)
            dao.insertChapters(updatedChapters)

            // Verify reading progress and bookmarks are completely preserved
            val progressAfter = dao.getProgressForMangaSync(mangaName)
            assertNotNull(progressAfter)
            assertEquals("Chapter 50.cbz", progressAfter?.chapterFileName)
            assertEquals(12, progressAfter?.pageIndex)
            assertTrue(dao.isChapterBookmarkedSync(mangaName, "Chapter 50.cbz"))
        }
    }
}
