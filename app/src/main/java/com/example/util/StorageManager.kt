package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.model.Chapter
import com.example.model.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class SafFileMetadata(
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val uri: Uri
)

object StorageManager {

    private const val TAG = "StorageManager"
    const val READER_DIR_NAME = "Reader"

    /**
     * Obtains persistent SAF permission and locates/creates the 'Reader' subfolder.
     * Returns the Uri of the 'Reader' folder.
     */
    suspend fun setupReaderFolder(context: Context, treeUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to take persistable permission: ${e.message}")
            }

            val pickedDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null

            // If the user picked a directory already named "Reader" (case-insensitive)
            if (pickedDir.name?.equals(READER_DIR_NAME, ignoreCase = true) == true) {
                return@withContext pickedDir.uri
            }

            // Check if a child folder named "Reader" already exists
            val existingReaderDir = pickedDir.findFile(READER_DIR_NAME)
            if (existingReaderDir != null && existingReaderDir.isDirectory) {
                return@withContext existingReaderDir.uri
            }

            // Otherwise, create the Reader directory inside the chosen location
            val createdDir = pickedDir.createDirectory(READER_DIR_NAME)
            return@withContext createdDir?.uri ?: pickedDir.uri
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupReaderFolder: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Checks whether we still have active permission for the given Uri.
     */
    fun hasPermission(context: Context, uri: Uri): Boolean {
        try {
            val permissions = context.contentResolver.persistedUriPermissions
            for (perm in permissions) {
                if (perm.isReadPermission) {
                    if (perm.uri == uri || uri.toString().startsWith(perm.uri.toString())) {
                        return true
                    }
                }
            }
            // Also verify DocumentFile can still be accessed
            val doc = DocumentFile.fromTreeUri(context, uri)
            return doc != null && doc.exists() && doc.canRead()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking permission for $uri: ${e.message}")
            return false
        }
    }

    /**
     * High-performance single-query directory enumeration using DocumentsContract.
     * Avoids N-individual Binder IPC queries caused by DocumentFile.listFiles() + isFile + name + length.
     */
    fun queryChildrenFast(context: Context, folderUri: Uri): List<SafFileMetadata> {
        val results = mutableListOf<SafFileMetadata>()
        try {
            val documentId = when {
                DocumentsContract.isDocumentUri(context, folderUri) -> DocumentsContract.getDocumentId(folderUri)
                else -> DocumentsContract.getTreeDocumentId(folderUri)
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, documentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx != -1) cursor.getString(idIdx) else null ?: continue
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null ?: continue
                    val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) else null
                    val isDir = DocumentsContract.Document.MIME_TYPE_DIR == mime
                    val size = if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val lastMod = if (modIdx != -1 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)

                    results.add(
                        SafFileMetadata(
                            documentId = docId,
                            name = name,
                            isDirectory = isDir,
                            size = size,
                            lastModified = lastMod,
                            uri = docUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fast directory query failed on $folderUri: ${e.message}. Using DocumentFile fallback.")
            // Resilient fallback to DocumentFile for providers without child-document tree query support
            val doc = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
            val files = doc.listFiles()
            for (f in files) {
                val name = f.name ?: continue
                results.add(
                    SafFileMetadata(
                        documentId = f.uri.toString(),
                        name = name,
                        isDirectory = f.isDirectory,
                        size = f.length(),
                        lastModified = f.lastModified(),
                        uri = f.uri
                    )
                )
            }
        }
        return results
    }

    /**
     * Enumerates chapter archives (.zip/.cbz) inside a manga directory without opening any archives.
     */
    suspend fun queryChapterFilesFast(
        context: Context,
        mangaFolderUri: Uri
    ): List<SafFileMetadata> = withContext(Dispatchers.IO) {
        val children = queryChildrenFast(context, mangaFolderUri)
        return@withContext children.filter { meta ->
            !meta.isDirectory && isSupportedChapter(meta.name)
        }
    }

    /**
     * Scans the Reader directory for manga subfolders using single-query SAF operations.
     * Hides folders that have no supported ZIP or CBZ chapters.
     */
    suspend fun scanMangaLibraryFast(
        context: Context,
        readerFolderUri: Uri
    ): List<Manga> = withContext(Dispatchers.IO) {
        val rootEntries = queryChildrenFast(context, readerFolderUri)
        val mangaFolders = rootEntries.filter { it.isDirectory }
        val mangaList = mutableListOf<Manga>()

        for (folder in mangaFolders) {
            // Count chapters without opening any archives
            val chapters = queryChapterFilesFast(context, folder.uri)
            if (chapters.isNotEmpty()) {
                mangaList.add(
                    Manga(
                        name = folder.name,
                        folderUri = folder.uri,
                        chapterCount = chapters.size
                    )
                )
            }
        }

        mangaList.sortBy { it.name.lowercase(Locale.ROOT) }
        return@withContext mangaList
    }

    fun isSupportedChapter(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase(Locale.ROOT)
        return lower.endsWith(".zip") || lower.endsWith(".cbz")
    }
}
