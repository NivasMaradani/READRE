package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ArchiveManager {

    private const val TAG = "ArchiveManager"
    private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    // Mutex per chapter hash to prevent concurrent access conflicts
    private val chapterLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * Checks if a filename is a supported image format.
     * Rejects directories, hidden dot-files, macOS metadata, XML, and non-image files.
     */
    fun isSupportedImage(fileName: String): Boolean {
        val normalized = fileName.replace('\\', '/').trim()
        val cleanName = normalized.lowercase(Locale.ROOT)
        val simpleName = cleanName.substringAfterLast('/')

        if (cleanName.startsWith("__macosx") ||
            cleanName.contains("/__macosx") ||
            simpleName.startsWith(".") ||
            simpleName.endsWith(".ds_store") ||
            simpleName.endsWith("thumbs.db") ||
            simpleName.endsWith(".nomedia") ||
            simpleName.endsWith(".xml") ||
            simpleName.endsWith(".txt") ||
            simpleName.endsWith(".json")
        ) {
            return false
        }
        val ext = simpleName.substringAfterLast('.', "")
        return ext in SUPPORTED_IMAGE_EXTENSIONS
    }

    /**
     * Extracts images from a ZIP or CBZ chapter archive into a temporary cache directory.
     * Sorts the resulting image files using natural numeric order.
     * Returns a list of cached image files.
     */
    suspend fun getOrExtractChapterPages(
        context: Context,
        chapterUri: Uri,
        mangaName: String,
        chapterFileName: String,
        forceReextract: Boolean = false
    ): List<File> = withContext(Dispatchers.IO) {
        val chapterHash = hashKey("$mangaName/$chapterFileName")
        val lock = chapterLocks.computeIfAbsent(chapterHash) { Mutex() }

        lock.withLock {
            val chapterCacheDir = File(context.cacheDir, "chapters/$chapterHash")
            val completeMarker = File(chapterCacheDir, ".complete")

            // 1. Check if already extracted and contains valid images
            if (!forceReextract && chapterCacheDir.exists() && completeMarker.exists() && chapterCacheDir.isDirectory) {
                val existingImages = chapterCacheDir.listFiles { file ->
                    file.isFile && isSupportedImage(file.name)
                }?.sortedWith { f1, f2 ->
                    NaturalOrderComparator.compare(f1.name, f2.name)
                } ?: emptyList()

                if (existingImages.isNotEmpty()) {
                    Log.d(TAG, "Reusing cached chapter '$chapterFileName' with ${existingImages.size} pages")
                    return@withLock existingImages
                }
            }

            // Clean up incomplete/corrupted cache directory
            if (chapterCacheDir.exists()) {
                chapterCacheDir.deleteRecursively()
            }
            chapterCacheDir.mkdirs()

            // Clean up older chapter caches to avoid disk accumulation
            cleanOldChapterCaches(context, currentChapterHash = chapterHash)

            Log.i(TAG, "Extracting chapter '$chapterFileName' for manga '$mangaName' from $chapterUri")

            // 2. Open archive input stream from ContentResolver
            val inputStream = try {
                context.contentResolver.openInputStream(chapterUri)
            } catch (e: Exception) {
                Log.e(TAG, """
                    [ARCHIVE_STORAGE_FAILURE]
                    Manga: $mangaName
                    Chapter: $chapterFileName
                    Uri: $chapterUri
                    Exception: ${e.javaClass.name} - ${e.message}
                """.trimIndent(), e)
                throw IllegalStateException("Cannot access archive '$chapterFileName'. Storage permissions may have expired or file was moved.", e)
            } ?: throw IllegalStateException("Could not open archive input stream for '$chapterFileName'")

            // 3. Copy to temporary file in cacheDir so ZipFile can read Central Directory with random access
            val tempArchive = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$chapterHash.tmp")
            val extractedFiles = mutableListOf<File>()

            try {
                inputStream.use { input ->
                    FileOutputStream(tempArchive).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }

                if (!tempArchive.exists() || tempArchive.length() == 0L) {
                    throw IllegalStateException("Archive file '$chapterFileName' is empty (0 bytes).")
                }

                Log.d(TAG, "Copied archive to temp file (${tempArchive.length()} bytes)")

                // 4. Try opening using ZipFile (with UTF-8, then fallback to ISO-8859-1 for non-standard encodings)
                var zipFile: ZipFile? = null
                try {
                    zipFile = try {
                        ZipFile(tempArchive, Charsets.UTF_8)
                    } catch (e: Exception) {
                        Log.w(TAG, "ZipFile with UTF-8 failed (${e.message}), trying ISO-8859-1")
                        ZipFile(tempArchive, Charset.forName("ISO-8859-1"))
                    }

                    val imageEntries = mutableListOf<ZipEntry>()
                    val allEntries = zipFile.entries()
                    var totalEntries = 0

                    while (allEntries.hasMoreElements()) {
                        val entry = allEntries.nextElement()
                        totalEntries++
                        val normalizedName = entry.name.replace('\\', '/')
                        if (!entry.isDirectory && isSupportedImage(normalizedName)) {
                            imageEntries.add(entry)
                        }
                    }

                    Log.d(TAG, "Found $totalEntries total entries, ${imageEntries.size} supported image entries in '$chapterFileName'")

                    if (imageEntries.isEmpty()) {
                        if (totalEntries == 0) {
                            throw IllegalStateException("Archive '$chapterFileName' contains no files (empty archive).")
                        } else {
                            throw IllegalStateException("Archive '$chapterFileName' contains no supported image files (.jpg, .png, .webp, .gif). Found $totalEntries non-image files.")
                        }
                    }

                    // Natural numeric sort on entry names, stripping folder path for sorting
                    imageEntries.sortWith { e1, e2 ->
                        val name1 = e1.name.replace('\\', '/').substringAfterLast('/')
                        val name2 = e2.name.replace('\\', '/').substringAfterLast('/')
                        NaturalOrderComparator.compare(name1, name2)
                    }

                    // Extract each image file in natural order with unique collision-free filenames
                    for ((index, entry) in imageEntries.withIndex()) {
                        val originalName = entry.name.replace('\\', '/').substringAfterLast('/')
                        val ext = originalName.substringAfterLast('.', "jpg").lowercase(Locale.ROOT)
                        val outputFile = File(chapterCacheDir, String.format(Locale.ROOT, "page_%05d.%s", index, ext))

                        try {
                            zipFile.getInputStream(entry).use { entryStream ->
                                FileOutputStream(outputFile).use { fileOut ->
                                    entryStream.copyTo(fileOut)
                                    fileOut.flush()
                                }
                            }

                            // Diagnostic validation
                            if (!outputFile.exists() || outputFile.length() == 0L) {
                                Log.w(TAG, """
                                    [ARCHIVE_ENTRY_WARNING]
                                    Manga: $mangaName
                                    Chapter: $chapterFileName
                                    PageIndex: $index
                                    EntryName: ${entry.name}
                                    Reason: Extracted 0 bytes
                                """.trimIndent())
                            } else {
                                val detectedFormat = ImageFormatDetector.detectFormat(outputFile)
                                if (detectedFormat == ImageFormatDetector.Format.UNKNOWN) {
                                    Log.w(TAG, "Page $index (${entry.name}) header does not match standard image signatures.")
                                }
                            }
                        } catch (entryEx: Exception) {
                            Log.e(TAG, """
                                [ARCHIVE_ENTRY_EXTRACTION_FAILURE]
                                Manga: $mangaName
                                Chapter: $chapterFileName
                                PageIndex: $index
                                EntryName: ${entry.name}
                                Exception: ${entryEx.javaClass.name} - ${entryEx.message}
                            """.trimIndent(), entryEx)
                        }

                        // Always preserve page ordering and index alignment
                        if (outputFile.exists()) {
                            extractedFiles.add(outputFile)
                        }
                    }

                } catch (e: Exception) {
                    if (e is IllegalStateException) throw e
                    Log.w(TAG, "ZipFile extraction failed: ${e.message}. Trying streaming ZipInputStream fallback.")
                    // Fallback: Streaming extraction via ZipInputStream
                    extractedFiles.clear()
                    tempArchive.inputStream().use { fileInput ->
                        ZipInputStream(BufferedInputStream(fileInput)).use { zipStream ->
                            var entry: ZipEntry? = zipStream.nextEntry
                            var index = 0
                            while (entry != null) {
                                val entryName = entry.name.replace('\\', '/')
                                if (!entry.isDirectory && isSupportedImage(entryName)) {
                                    val ext = entryName.substringAfterLast('.', "jpg").lowercase(Locale.ROOT)
                                    val outputFile = File(chapterCacheDir, String.format(Locale.ROOT, "page_%05d.%s", index, ext))
                                    try {
                                        FileOutputStream(outputFile).use { out ->
                                            zipStream.copyTo(out)
                                            out.flush()
                                        }
                                        if (outputFile.exists()) {
                                            extractedFiles.add(outputFile)
                                            index++
                                        }
                                    } catch (ex: Exception) {
                                        Log.e(TAG, "Fallback extraction failed for entry $entryName: ${ex.message}")
                                    }
                                }
                                zipStream.closeEntry()
                                entry = zipStream.nextEntry
                            }
                        }
                    }

                    // Sort fallback files naturally
                    extractedFiles.sortWith { f1, f2 ->
                        NaturalOrderComparator.compare(f1.name, f2.name)
                    }
                } finally {
                    try {
                        zipFile?.close()
                    } catch (ignored: Exception) {}
                }

                if (extractedFiles.isEmpty()) {
                    throw IllegalStateException("No valid images could be decoded from archive '$chapterFileName'.")
                }

                // Create completion marker
                completeMarker.createNewFile()
                Log.i(TAG, "Successfully extracted and prepared ${extractedFiles.size} pages for '$chapterFileName'")

            } catch (e: Exception) {
                Log.e(TAG, "Error unpacking archive '$chapterFileName': ${e.message}", e)
                throw e
            } finally {
                // Clean up temporary archive file
                if (tempArchive.exists()) {
                    tempArchive.delete()
                }
            }

            return@withLock extractedFiles
        }
    }

    /**
     * Re-extracts a specific page or the whole chapter if a page was corrupted or incomplete.
     */
    suspend fun reextractChapter(
        context: Context,
        chapterUri: Uri,
        mangaName: String,
        chapterFileName: String
    ): List<File> {
        return getOrExtractChapterPages(
            context = context,
            chapterUri = chapterUri,
            mangaName = mangaName,
            chapterFileName = chapterFileName,
            forceReextract = true
        )
    }

    /**
     * Extracts the first image of a chapter to use as a cover thumbnail.
     * Cached under cache/covers/<manga_hash>.jpg
     */
    suspend fun getOrExtractCoverThumbnail(
        context: Context,
        chapterUri: Uri,
        mangaName: String
    ): File? = withContext(Dispatchers.IO) {
        val mangaHash = hashKey(mangaName)
        val coversDir = File(context.cacheDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val coverFile = File(coversDir, "$mangaHash.jpg")
        if (coverFile.exists() && coverFile.length() > 0L) {
            return@withContext coverFile
        }

        try {
            val inputStream = context.contentResolver.openInputStream(chapterUri) ?: return@withContext null
            ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry
                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/')
                    if (!entry.isDirectory && isSupportedImage(normalizedName)) {
                        FileOutputStream(coverFile).use { output ->
                            zipStream.copyTo(output)
                            output.flush()
                        }
                        if (coverFile.exists() && coverFile.length() > 0L) {
                            return@withContext coverFile
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract cover for $mangaName: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Limit cached chapters to avoid excessive disk usage.
     * Keeps up to 5 most recently used chapter caches to avoid purging active continuous stream chapters.
     */
    private fun cleanOldChapterCaches(context: Context, currentChapterHash: String) {
        try {
            val chaptersDir = File(context.cacheDir, "chapters")
            if (!chaptersDir.exists() || !chaptersDir.isDirectory) return

            val dirs = chaptersDir.listFiles { file -> file.isDirectory } ?: return
            // Keep up to 5 most recently modified chapters
            if (dirs.size > 5) {
                val toDelete = dirs.filter { it.name != currentChapterHash }
                    .sortedBy { it.lastModified() }
                    .dropLast(4)

                for (dir in toDelete) {
                    dir.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old chapter caches: ${e.message}")
        }
    }

    private fun hashKey(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
