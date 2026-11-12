package com.example.model

import android.net.Uri

enum class ReadingMode {
    SINGLE_PAGE,
    VERTICAL_CONTINUOUS
}

enum class ReadingDirection {
    RIGHT_TO_LEFT, // RTL (default)
    LEFT_TO_RIGHT  // LTR
}

data class Manga(
    val name: String,
    val folderUri: Uri,
    val chapterCount: Int,
    val coverThumbnailUri: Uri? = null,
    val coverFilePath: String? = null,
    val hasCustomCover: Boolean = false,
    val isHidden: Boolean = false
)

data class Chapter(
    val originalFileName: String,
    val displayName: String,
    val fileUri: Uri,
    val mangaName: String,
    val isBookmarked: Boolean = false,
    val fileSize: Long = 0L,
    val sortOrder: Int = 0,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false,
    val lastReadTimestamp: Long? = null
)
