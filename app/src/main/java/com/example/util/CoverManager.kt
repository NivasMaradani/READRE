package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Handles automatic manga cover thumbnail generation with downsampling,
 * caching in persistent private storage, and user-selected custom cover management.
 */
object CoverManager {

    private const val TAG = "CoverManager"
    private const val TARGET_THUMBNAIL_WIDTH = 320
    private const val TARGET_THUMBNAIL_HEIGHT = 480
    private const val TARGET_CUSTOM_MAX_DIMEN = 960

    private val coverDispatcher = Dispatchers.IO.limitedParallelism(2)

    private fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun hashKey(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolves the active cover file for a manga:
     * 1. Returns custom cover if it exists.
     * 2. Else returns auto-generated cover if it exists.
     * 3. Else returns null.
     */
    fun getCoverFile(context: Context, mangaName: String): File? {
        val hash = hashKey(mangaName)
        val coversDir = getCoversDir(context)

        val customFile = File(coversDir, "custom_$hash.jpg")
        if (customFile.exists() && customFile.length() > 0L) {
            return customFile
        }

        val autoFile = File(coversDir, "auto_$hash.jpg")
        if (autoFile.exists() && autoFile.length() > 0L) {
            return autoFile
        }

        return null
    }

    fun hasCustomCover(context: Context, mangaName: String): Boolean {
        val hash = hashKey(mangaName)
        val customFile = File(getCoversDir(context), "custom_$hash.jpg")
        return customFile.exists() && customFile.length() > 0L
    }

    /**
     * Asynchronously generates an automatic cover thumbnail from the first chapter's first image.
     * Uses downsampling (inSampleSize) to prevent high memory usage.
     * Does NOT permanently extract the chapter archive.
     */
    suspend fun getOrGenerateAutoCover(
        context: Context,
        mangaName: String,
        firstChapterUri: Uri,
        forceRegenerate: Boolean = false
    ): File? = withContext(coverDispatcher) {
        val hash = hashKey(mangaName)
        val coversDir = getCoversDir(context)
        val autoFile = File(coversDir, "auto_$hash.jpg")

        if (!forceRegenerate && autoFile.exists() && autoFile.length() > 0L) {
            return@withContext autoFile
        }

        if (forceRegenerate && autoFile.exists()) {
            autoFile.delete()
        }

        Log.d(TAG, "Generating auto cover for '$mangaName' from $firstChapterUri (force=$forceRegenerate)")

        try {
            // Pass 1: Quick inspection of archive entries to identify the first naturally sorted image
            val candidateEntries = mutableListOf<String>()
            context.contentResolver.openInputStream(firstChapterUri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (!entry.isDirectory && ArchiveManager.isSupportedImage(name)) {
                            candidateEntries.add(entry.name)
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            if (candidateEntries.isEmpty()) {
                Log.w(TAG, "No supported images found in first chapter for '$mangaName'")
                return@withContext null
            }

            // Natural numeric sort on entry filenames (e.g. 01.jpg, 02.jpg, 10.jpg)
            candidateEntries.sortWith { e1, e2 ->
                val name1 = e1.replace('\\', '/').substringAfterLast('/')
                val name2 = e2.replace('\\', '/').substringAfterLast('/')
                NaturalOrderComparator.compare(name1, name2)
            }
            val targetEntryName = candidateEntries.first()

            // Pass 2: Extract only the target first image into a temp file and downsample
            val tempEntryFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}_$hash.tmp")
            try {
                context.contentResolver.openInputStream(firstChapterUri)?.use { input ->
                    ZipInputStream(BufferedInputStream(input)).use { zipStream ->
                        var entry: ZipEntry? = zipStream.nextEntry
                        while (entry != null) {
                            if (entry.name == targetEntryName) {
                                FileOutputStream(tempEntryFile).use { out ->
                                    zipStream.copyTo(out)
                                    out.flush()
                                }
                                break
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }

                if (tempEntryFile.exists() && tempEntryFile.length() > 0L) {
                    val downsampledBitmap = decodeSampledBitmapFromFile(
                        tempEntryFile.absolutePath,
                        TARGET_THUMBNAIL_WIDTH,
                        TARGET_THUMBNAIL_HEIGHT
                    )

                    if (downsampledBitmap != null) {
                        FileOutputStream(autoFile).use { fileOut ->
                            downsampledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOut)
                            fileOut.flush()
                        }
                        downsampledBitmap.recycle()
                        Log.i(TAG, "Generated auto cover for '$mangaName' from '$targetEntryName' (${autoFile.length()} bytes)")
                        return@withContext autoFile
                    }
                }
            } finally {
                if (tempEntryFile.exists()) {
                    tempEntryFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate auto cover for '$mangaName': ${e.message}")
        }

        return@withContext null
    }

    /**
     * Forces regeneration of the automatic cover thumbnail from the first chapter.
     */
    suspend fun regenerateAutoCover(
        context: Context,
        mangaName: String,
        firstChapterUri: Uri
    ): File? = getOrGenerateAutoCover(context, mangaName, firstChapterUri, forceRegenerate = true)

    /**
     * Replaces the manga cover with a user-selected custom image.
     * Decodes with downsampling to bounded dimensions, then saves to custom_<hash>.jpg.
     */
    suspend fun setCustomCover(
        context: Context,
        mangaName: String,
        imageUri: Uri
    ): File? = withContext(coverDispatcher) {
        val hash = hashKey(mangaName)
        val coversDir = getCoversDir(context)
        val customFile = File(coversDir, "custom_$hash.jpg")

        Log.d(TAG, "Setting custom cover for '$mangaName' from $imageUri")

        var inputStream: InputStream? = null
        val tempFile = File(context.cacheDir, "temp_custom_${System.currentTimeMillis()}_$hash.tmp")
        try {
            inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
                out.flush()
            }

            if (tempFile.exists() && tempFile.length() > 0L) {
                val bitmap = decodeSampledBitmapFromFile(
                    tempFile.absolutePath,
                    TARGET_CUSTOM_MAX_DIMEN,
                    TARGET_CUSTOM_MAX_DIMEN
                )

                if (bitmap != null) {
                    FileOutputStream(customFile).use { fileOut ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOut)
                        fileOut.flush()
                    }
                    bitmap.recycle()
                    Log.i(TAG, "Successfully saved custom cover for '$mangaName'")
                    return@withContext customFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom cover for '$mangaName': ${e.message}", e)
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: Exception) {}
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        return@withContext null
    }

    /**
     * Resets any custom cover for the given manga, deleting custom_<hash>.jpg.
     * Subsequent calls to getCoverFile will fall back to the auto-generated cover.
     */
    suspend fun resetCustomCover(
        context: Context,
        mangaName: String
    ): Boolean = withContext(coverDispatcher) {
        val hash = hashKey(mangaName)
        val customFile = File(getCoversDir(context), "custom_$hash.jpg")
        if (customFile.exists()) {
            val deleted = customFile.delete()
            Log.i(TAG, "Custom cover reset for '$mangaName': deleted=$deleted")
            return@withContext deleted
        }
        return@withContext true
    }

    /**
     * Decodes a downsampled bitmap from a file path using inSampleSize.
     */
    fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Low memory footprint

        return try {
            BitmapFactory.decodeFile(filePath, options)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM decoding cover: ${oom.message}")
            try {
                options.inSampleSize *= 2
                BitmapFactory.decodeFile(filePath, options)
            } catch (e: Throwable) {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding cover file: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize.coerceAtLeast(1)
    }
}
