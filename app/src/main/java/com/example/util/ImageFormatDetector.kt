package com.example.util

import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

object ImageFormatDetector {

    private const val TAG = "ImageFormatDetector"

    enum class Format(val extension: String, val mimeType: String) {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp"),
        GIF("gif", "image/gif"),
        BMP("bmp", "image/bmp"),
        UNKNOWN("", "application/octet-stream")
    }

    /**
     * Inspects magic bytes from the first 12 bytes of a stream or byte array.
     */
    fun detectFormat(headerBytes: ByteArray): Format {
        if (headerBytes.size < 3) return Format.UNKNOWN

        // JPEG: 0xFF, 0xD8, 0xFF
        if (headerBytes[0] == 0xFF.toByte() &&
            headerBytes[1] == 0xD8.toByte() &&
            headerBytes[2] == 0xFF.toByte()
        ) {
            return Format.JPEG
        }

        // PNG: 0x89, 'P', 'N', 'G' (0x89, 0x50, 0x4E, 0x47)
        if (headerBytes.size >= 4 &&
            headerBytes[0] == 0x89.toByte() &&
            headerBytes[1] == 0x50.toByte() &&
            headerBytes[2] == 0x4E.toByte() &&
            headerBytes[3] == 0x47.toByte()
        ) {
            return Format.PNG
        }

        // GIF: "GIF87a" or "GIF89a"
        if (headerBytes.size >= 6 &&
            headerBytes[0] == 'G'.code.toByte() &&
            headerBytes[1] == 'I'.code.toByte() &&
            headerBytes[2] == 'F'.code.toByte() &&
            headerBytes[3] == '8'.code.toByte() &&
            (headerBytes[4] == '7'.code.toByte() || headerBytes[4] == '9'.code.toByte()) &&
            headerBytes[5] == 'a'.code.toByte()
        ) {
            return Format.GIF
        }

        // WEBP: "RIFF" .... "WEBP"
        if (headerBytes.size >= 12 &&
            headerBytes[0] == 'R'.code.toByte() &&
            headerBytes[1] == 'I'.code.toByte() &&
            headerBytes[2] == 'F'.code.toByte() &&
            headerBytes[3] == 'F'.code.toByte() &&
            headerBytes[8] == 'W'.code.toByte() &&
            headerBytes[9] == 'E'.code.toByte() &&
            headerBytes[10] == 'B'.code.toByte() &&
            headerBytes[11] == 'P'.code.toByte()
        ) {
            return Format.WEBP
        }

        // BMP: "BM"
        if (headerBytes.size >= 2 &&
            headerBytes[0] == 'B'.code.toByte() &&
            headerBytes[1] == 'M'.code.toByte()
        ) {
            return Format.BMP
        }

        return Format.UNKNOWN
    }

    /**
     * Reads header bytes from a file to detect its actual format.
     */
    fun detectFormat(file: File): Format {
        if (!file.exists() || file.length() < 3L) return Format.UNKNOWN
        return try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(16)
                val read = input.read(buffer)
                if (read > 0) detectFormat(buffer.copyOf(read)) else Format.UNKNOWN
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting format for ${file.name}: ${e.message}")
            Format.UNKNOWN
        }
    }

    /**
     * Checks if a file name or extension indicates a supported image format.
     */
    fun hasSupportedExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }

    /**
     * Checks if a file has a supported format based on magic bytes.
     */
    fun isSupportedImage(file: File): Boolean {
        return detectFormat(file) != Format.UNKNOWN
    }

    /**
     * Diagnoses why an image file failed to load.
     * Returns a concise, user-friendly reason message.
     */
    fun diagnoseFailure(file: File, throwable: Throwable?): String {
        if (!file.exists()) {
            return "File not found or storage access error"
        }
        if (file.length() == 0L) {
            return "File is empty (0 bytes)"
        }
        if (throwable is OutOfMemoryError) {
            return "Insufficient memory"
        }

        val format = detectFormat(file)
        if (format == Format.UNKNOWN) {
            return "Unsupported image format or corrupted header"
        }

        // Test decoder with bounds decoding
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return "Image decoding failed"
            }
        } catch (oom: OutOfMemoryError) {
            return "Insufficient memory"
        } catch (e: Exception) {
            return "Image decoding failed: ${e.message ?: "corrupted data"}"
        }

        val causeMsg = throwable?.message?.lowercase(Locale.ROOT) ?: ""
        if (causeMsg.contains("oom") || causeMsg.contains("out of memory")) {
            return "Insufficient memory"
        }
        if (causeMsg.contains("decode") || causeMsg.contains("corrupt")) {
            return "Image decoding failed"
        }

        return "Image loading error"
    }
}
