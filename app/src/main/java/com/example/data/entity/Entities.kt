package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Manga-level reading progress pointer for "Continue Reading"
 */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    val mangaName: String,
    val chapterFileName: String,
    val pageIndex: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Independent reading progress record for every chapter
 */
@Entity(
    tableName = "chapter_reading_progress",
    primaryKeys = ["mangaName", "chapterFileName"],
    indices = [
        Index(value = ["mangaName"]),
        Index(value = ["lastReadTimestamp"])
    ]
)
data class ChapterReadingProgressEntity(
    val mangaName: String,
    val chapterFileName: String,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["mangaName", "chapterFileName"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaName: String,
    val chapterFileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(
    tableName = "indexed_chapters",
    primaryKeys = ["mangaName", "chapterFileName"],
    indices = [
        Index(value = ["mangaName"]),
        Index(value = ["mangaName", "sortOrder"])
    ]
)
data class ChapterEntity(
    val mangaName: String,
    val chapterFileName: String,
    val displayName: String,
    val documentUri: String,
    val fileSize: Long = 0L,
    val lastModified: Long = 0L,
    val sortOrder: Int = 0,
    val lastScannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "indexed_manga")
data class MangaEntity(
    @PrimaryKey
    val name: String,
    val folderUri: String,
    val chapterCount: Int = 0,
    val lastModified: Long = 0L,
    val lastScannedAt: Long = System.currentTimeMillis(),
    val customCoverPath: String? = null,
    val autoCoverPath: String? = null,
    val isHidden: Boolean = false
)
