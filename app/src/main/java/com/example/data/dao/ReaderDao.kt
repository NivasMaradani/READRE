package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.MangaEntity
import com.example.data.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderDao {

    // --- Manga-Level Reading Progress (Continue Reading Pointer) ---
    @Query("SELECT * FROM reading_progress WHERE mangaName NOT IN (SELECT name FROM indexed_manga WHERE isHidden = 1) ORDER BY timestamp DESC")
    fun getAllReadingProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE mangaName = :mangaName LIMIT 1")
    fun getProgressForManga(mangaName: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE mangaName = :mangaName LIMIT 1")
    suspend fun getProgressForMangaSync(mangaName: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE mangaName = :mangaName")
    suspend fun deleteProgressForManga(mangaName: String)

    // --- Per-Chapter Reading Progress & History ---
    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName NOT IN (SELECT name FROM indexed_manga WHERE isHidden = 1) ORDER BY lastReadTimestamp DESC")
    fun getAllChapterProgress(): Flow<List<ChapterReadingProgressEntity>>

    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName NOT IN (SELECT name FROM indexed_manga WHERE isHidden = 1) ORDER BY lastReadTimestamp DESC")
    suspend fun getAllChapterProgressSync(): List<ChapterReadingProgressEntity>

    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName = :mangaName")
    fun getChapterProgressForManga(mangaName: String): Flow<List<ChapterReadingProgressEntity>>

    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName = :mangaName")
    suspend fun getChapterProgressForMangaSync(mangaName: String): List<ChapterReadingProgressEntity>

    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName LIMIT 1")
    fun getChapterProgress(mangaName: String, chapterFileName: String): Flow<ChapterReadingProgressEntity?>

    @Query("SELECT * FROM chapter_reading_progress WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName LIMIT 1")
    suspend fun getChapterProgressSync(mangaName: String, chapterFileName: String): ChapterReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapterProgress(progress: ChapterReadingProgressEntity)

    @Query("UPDATE chapter_reading_progress SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName")
    suspend fun updateChapterCompletion(mangaName: String, chapterFileName: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chapter_reading_progress WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName")
    suspend fun deleteChapterProgress(mangaName: String, chapterFileName: String)

    @Query("DELETE FROM chapter_reading_progress")
    suspend fun clearAllChapterProgress()

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks WHERE mangaName NOT IN (SELECT name FROM indexed_manga WHERE isHidden = 1) ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE mangaName = :mangaName ORDER BY timestamp DESC")
    fun getBookmarksForManga(mangaName: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName)")
    fun isChapterBookmarked(mangaName: String, chapterFileName: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName)")
    suspend fun isChapterBookmarkedSync(mangaName: String, chapterFileName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE mangaName = :mangaName AND chapterFileName = :chapterFileName")
    suspend fun deleteBookmark(mangaName: String, chapterFileName: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    // --- Settings ---
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingSync(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)

    // --- Chapter Indexing (Large Libraries) ---
    @Query("SELECT * FROM indexed_chapters WHERE mangaName = :mangaName ORDER BY sortOrder ASC")
    fun getChaptersForMangaFlow(mangaName: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM indexed_chapters WHERE mangaName = :mangaName ORDER BY sortOrder ASC")
    suspend fun getChaptersForMangaSync(mangaName: String): List<ChapterEntity>

    @Query("SELECT COUNT(*) FROM indexed_chapters WHERE mangaName = :mangaName")
    suspend fun getChapterCountForManga(mangaName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM indexed_chapters WHERE mangaName = :mangaName AND chapterFileName IN (:fileNames)")
    suspend fun deleteChapters(mangaName: String, fileNames: List<String>)

    @Query("DELETE FROM indexed_chapters WHERE mangaName = :mangaName")
    suspend fun deleteAllChaptersForManga(mangaName: String)

    // --- Manga Indexing ---
    @Query("SELECT * FROM indexed_manga ORDER BY LOWER(name) ASC")
    fun getAllMangaFlow(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM indexed_manga ORDER BY LOWER(name) ASC")
    suspend fun getAllMangaSync(): List<MangaEntity>

    @Query("SELECT * FROM indexed_manga WHERE isHidden = 0 ORDER BY LOWER(name) ASC")
    fun getVisibleMangaFlow(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM indexed_manga WHERE isHidden = 0 ORDER BY LOWER(name) ASC")
    suspend fun getVisibleMangaSync(): List<MangaEntity>

    @Query("SELECT * FROM indexed_manga WHERE isHidden = 1 ORDER BY LOWER(name) ASC")
    fun getHiddenMangaFlow(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM indexed_manga WHERE isHidden = 1 ORDER BY LOWER(name) ASC")
    suspend fun getHiddenMangaSync(): List<MangaEntity>

    @Query("SELECT * FROM indexed_manga WHERE name = :name LIMIT 1")
    suspend fun getMangaByName(name: String): MangaEntity?

    @Query("SELECT isHidden FROM indexed_manga WHERE name = :name LIMIT 1")
    suspend fun isMangaHidden(name: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaList(mangaList: List<MangaEntity>)

    @Query("DELETE FROM indexed_manga WHERE name NOT IN (:names)")
    suspend fun deleteMissingManga(names: List<String>)

    @Query("DELETE FROM indexed_manga WHERE name = :name")
    suspend fun deleteMangaByName(name: String)

    @Query("UPDATE indexed_manga SET chapterCount = :count, lastScannedAt = :scannedAt WHERE name = :name")
    suspend fun updateMangaChapterCount(name: String, count: Int, scannedAt: Long = System.currentTimeMillis())

    @Query("UPDATE indexed_manga SET customCoverPath = :customPath WHERE name = :name")
    suspend fun updateMangaCustomCover(name: String, customPath: String?)

    @Query("UPDATE indexed_manga SET autoCoverPath = :autoPath WHERE name = :name")
    suspend fun updateMangaAutoCover(name: String, autoPath: String?)

    @Query("UPDATE indexed_manga SET isHidden = :isHidden WHERE name = :name")
    suspend fun setMangaHidden(name: String, isHidden: Boolean)
}
