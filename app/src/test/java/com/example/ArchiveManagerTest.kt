package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.util.ArchiveManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
class ArchiveManagerTest {

    @Test
    fun testIsSupportedImageFiltersValidAndInvalidEntries() {
        assertTrue(ArchiveManager.isSupportedImage("page_01.jpg"))
        assertTrue(ArchiveManager.isSupportedImage("page_02.JPEG"))
        assertTrue(ArchiveManager.isSupportedImage("comic.png"))
        assertTrue(ArchiveManager.isSupportedImage("scene.webp"))
        assertTrue(ArchiveManager.isSupportedImage("anim.gif"))

        assertFalse(ArchiveManager.isSupportedImage(".DS_Store"))
        assertFalse(ArchiveManager.isSupportedImage("Thumbs.db"))
        assertFalse(ArchiveManager.isSupportedImage("__MACOSX/image.jpg"))
        assertFalse(ArchiveManager.isSupportedImage("info.txt"))
        assertFalse(ArchiveManager.isSupportedImage("metadata.xml"))
        assertFalse(ArchiveManager.isSupportedImage("script.json"))
    }

    @Test
    fun testExtractingZipArchiveWithNaturalOrderAndNonImageFiltering() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val zipFile = File(context.cacheDir, "test_chapter.cbz")

            // Create sample CBZ archive
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add entries in non-sequential order
                listOf("10.jpg", "1.jpg", "readme.txt", "2.jpg", "Thumbs.db", "11.jpg").forEach { name ->
                    zos.putNextEntry(ZipEntry(name))
                    zos.write("fake image data $name".toByteArray())
                    zos.closeEntry()
                }
            }

            val pages = ArchiveManager.getOrExtractChapterPages(
                context = context,
                chapterUri = Uri.fromFile(zipFile),
                mangaName = "TestManga",
                chapterFileName = "test_chapter.cbz"
            )

            // Only image files should be extracted and naturally sorted
            assertEquals(4, pages.size)
            assertEquals("page_00000.jpg", pages[0].name)
            assertEquals("fake image data 1.jpg", pages[0].readText())

            assertEquals("page_00001.jpg", pages[1].name)
            assertEquals("fake image data 2.jpg", pages[1].readText())

            assertEquals("page_00002.jpg", pages[2].name)
            assertEquals("fake image data 10.jpg", pages[2].readText())

            assertEquals("page_00003.jpg", pages[3].name)
            assertEquals("fake image data 11.jpg", pages[3].readText())

            // Clean up
            zipFile.delete()
        }
    }

    @Test
    fun testEmptyArchiveThrowsMeaningfulException() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyZip = File(context.cacheDir, "empty.zip")
            ZipOutputStream(FileOutputStream(emptyZip)).use {
                // zero entries
            }

            try {
                ArchiveManager.getOrExtractChapterPages(
                    context = context,
                    chapterUri = Uri.fromFile(emptyZip),
                    mangaName = "EmptyManga",
                    chapterFileName = "empty.zip"
                )
                fail("Expected exception for empty archive")
            } catch (e: Exception) {
                val msg = e.message ?: ""
                assertTrue(msg.contains("empty") || msg.contains("no files") || msg.contains("no supported image"))
            } finally {
                emptyZip.delete()
            }
        }
    }

    @Test
    fun testCorruptedArchiveHandling() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val corruptedFile = File(context.cacheDir, "corrupted.cbz")
            corruptedFile.writeText("This is not a zip archive header.")

            try {
                ArchiveManager.getOrExtractChapterPages(
                    context = context,
                    chapterUri = Uri.fromFile(corruptedFile),
                    mangaName = "CorruptedManga",
                    chapterFileName = "corrupted.cbz"
                )
                fail("Expected exception for corrupted archive")
            } catch (e: Exception) {
                // Expected exception
                assertTrue(true)
            } finally {
                corruptedFile.delete()
            }
        }
    }
}
