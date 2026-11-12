package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.ReaderDatabase
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.MangaEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.model.Chapter
import com.example.model.Manga
import com.example.model.ReadingDirection
import com.example.model.ReadingMode
import com.example.util.ArchiveManager
import com.example.util.CoverManager
import com.example.util.NaturalOrderComparator
import com.example.util.SafFileMetadata
import com.example.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ReaderRepository(
    private val context: Context,
    private val database: ReaderDatabase
) {
    private val dao = database.readerDao()

    companion object {
        private const val TAG = "ReaderRepository"
        const val KEY_READER_FOLDER_URI = "reader_folder_uri"
        const val KEY_DEFAULT_READING_MODE = "default_reading_mode"
        const val KEY_DEFAULT_READING_DIRECTION = "default_reading_direction"
        const val KEY_CHAPTER_TRANSITIONS_ENABLED = "chapter_transitions_enabled"
        const val KEY_AUTO_LOAD_NEXT_CHAPTER = "auto_load_next_chapter"
        const val KEY_CHAPTER_TRANSITION_STYLE = "chapter_transition_style"
        const val KEY_HOLD_SCROLL_SPEED = "hold_scroll_speed"
        const val KEY_PAGED_RIGHT_TAP_ACTION = "paged_right_tap_action"
        const val KEY_PAGED_LEFT_TAP_ACTION = "paged_left_tap_action"
        const val KEY_REVERSE_PAGED_NAV = "reverse_paged_nav"
        const val KEY_HOLD_SCROLL_SPEED_PX = "hold_scroll_speed_px"
        const val KEY_HOLD_ACTIVATION_DELAY_MS = "hold_activation_delay_ms"
        const val KEY_REVERSE_HOLD_SCROLL_DIR = "reverse_hold_scroll_dir"
        const val KEY_VERTICAL_TAP_SCROLL_PERCENT = "vertical_tap_scroll_percent"
        const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"

        val scanDispatcher = Dispatchers.IO.limitedParallelism(4)
        val coverDispatcher = Dispatchers.IO.limitedParallelism(2)
    }

    private val mangaScanLocks = ConcurrentHashMap<String, Mutex>()

    // --- Settings / Preferences ---
    val readerFolderUriFlow: Flow<String?> = dao.getSetting(KEY_READER_FOLDER_URI)

    suspend fun getSavedReaderFolderUri(): String? = dao.getSettingSync(KEY_READER_FOLDER_URI)

    suspend fun saveReaderFolderUri(uri: String) {
        dao.saveSetting(AppSettingEntity(KEY_READER_FOLDER_URI, uri))
    }

    val defaultReadingModeFlow: Flow<ReadingMode> = dao.getSetting(KEY_DEFAULT_READING_MODE).map { value ->
        when (value) {
            ReadingMode.VERTICAL_CONTINUOUS.name -> ReadingMode.VERTICAL_CONTINUOUS
            else -> ReadingMode.SINGLE_PAGE
        }
    }

    suspend fun saveDefaultReadingMode(mode: ReadingMode) {
        dao.saveSetting(AppSettingEntity(KEY_DEFAULT_READING_MODE, mode.name))
    }

    val defaultReadingDirectionFlow: Flow<ReadingDirection> = dao.getSetting(KEY_DEFAULT_READING_DIRECTION).map { value ->
        when (value) {
            ReadingDirection.LEFT_TO_RIGHT.name -> ReadingDirection.LEFT_TO_RIGHT
            else -> ReadingDirection.RIGHT_TO_LEFT
        }
    }

    suspend fun saveDefaultReadingDirection(direction: ReadingDirection) {
        dao.saveSetting(AppSettingEntity(KEY_DEFAULT_READING_DIRECTION, direction.name))
    }

    val chapterTransitionsEnabledFlow: Flow<Boolean> = dao.getSetting(KEY_CHAPTER_TRANSITIONS_ENABLED).map { value ->
        value?.toBooleanStrictOrNull() ?: true // Default enabled
    }

    suspend fun saveChapterTransitionsEnabled(enabled: Boolean) {
        dao.saveSetting(AppSettingEntity(KEY_CHAPTER_TRANSITIONS_ENABLED, enabled.toString()))
    }

    val autoLoadNextChapterFlow: Flow<Boolean> = dao.getSetting(KEY_AUTO_LOAD_NEXT_CHAPTER).map { value ->
        value?.toBooleanStrictOrNull() ?: true // Default enabled
    }

    suspend fun saveAutoLoadNextChapter(enabled: Boolean) {
        dao.saveSetting(AppSettingEntity(KEY_AUTO_LOAD_NEXT_CHAPTER, enabled.toString()))
    }

    val chapterTransitionStyleFlow: Flow<String> = dao.getSetting(KEY_CHAPTER_TRANSITION_STYLE).map { value ->
        value ?: "SHOW_TITLE" // "SHOW_TITLE" or "MINIMAL"
    }

    suspend fun saveChapterTransitionStyle(style: String) {
        dao.saveSetting(AppSettingEntity(KEY_CHAPTER_TRANSITION_STYLE, style))
    }

    val holdScrollSpeedFlow: Flow<com.example.model.HoldScrollSpeed> = dao.getSetting(KEY_HOLD_SCROLL_SPEED).map { value ->
        com.example.model.HoldScrollSpeed.fromString(value)
    }

    suspend fun saveHoldScrollSpeed(speed: com.example.model.HoldScrollSpeed) {
        dao.saveSetting(AppSettingEntity(KEY_HOLD_SCROLL_SPEED, speed.name))
    }

    // Paged navigation actions
    val pagedRightTapActionFlow: Flow<com.example.model.TapAction> = dao.getSetting(KEY_PAGED_RIGHT_TAP_ACTION).map { value ->
        com.example.model.TapAction.fromString(value, default = com.example.model.TapAction.NEXT_PAGE)
    }

    suspend fun savePagedRightTapAction(action: com.example.model.TapAction) {
        dao.saveSetting(AppSettingEntity(KEY_PAGED_RIGHT_TAP_ACTION, action.name))
    }

    val pagedLeftTapActionFlow: Flow<com.example.model.TapAction> = dao.getSetting(KEY_PAGED_LEFT_TAP_ACTION).map { value ->
        com.example.model.TapAction.fromString(value, default = com.example.model.TapAction.PREVIOUS_PAGE)
    }

    suspend fun savePagedLeftTapAction(action: com.example.model.TapAction) {
        dao.saveSetting(AppSettingEntity(KEY_PAGED_LEFT_TAP_ACTION, action.name))
    }

    val reversePageNavigationFlow: Flow<Boolean> = dao.getSetting(KEY_REVERSE_PAGED_NAV).map { value ->
        value?.toBooleanStrictOrNull() ?: false
    }

    suspend fun saveReversePageNavigation(reverse: Boolean) {
        dao.saveSetting(AppSettingEntity(KEY_REVERSE_PAGED_NAV, reverse.toString()))
    }

    // Continuous vertical scrolling configuration
    val holdScrollSpeedPxFlow: Flow<Int> = dao.getSetting(KEY_HOLD_SCROLL_SPEED_PX).map { value ->
        value?.toIntOrNull()?.coerceIn(100, 2000) ?: 650
    }

    suspend fun saveHoldScrollSpeedPx(speedPx: Int) {
        dao.saveSetting(AppSettingEntity(KEY_HOLD_SCROLL_SPEED_PX, speedPx.coerceIn(100, 2000).toString()))
    }

    val holdActivationDelayMsFlow: Flow<Int> = dao.getSetting(KEY_HOLD_ACTIVATION_DELAY_MS).map { value ->
        value?.toIntOrNull()?.coerceIn(150, 800) ?: 300
    }

    suspend fun saveHoldActivationDelayMs(delayMs: Int) {
        dao.saveSetting(AppSettingEntity(KEY_HOLD_ACTIVATION_DELAY_MS, delayMs.coerceIn(150, 800).toString()))
    }

    val reverseHoldScrollDirectionFlow: Flow<Boolean> = dao.getSetting(KEY_REVERSE_HOLD_SCROLL_DIR).map { value ->
        value?.toBooleanStrictOrNull() ?: false
    }

    suspend fun saveReverseHoldScrollDirection(reverse: Boolean) {
        dao.saveSetting(AppSettingEntity(KEY_REVERSE_HOLD_SCROLL_DIR, reverse.toString()))
    }

    val verticalTapScrollPercentFlow: Flow<Int> = dao.getSetting(KEY_VERTICAL_TAP_SCROLL_PERCENT).map { value ->
        value?.toIntOrNull()?.coerceIn(20, 100) ?: 50
    }

    suspend fun saveVerticalTapScrollPercent(percent: Int) {
        dao.saveSetting(AppSettingEntity(KEY_VERTICAL_TAP_SCROLL_PERCENT, percent.coerceIn(20, 100).toString()))
    }

    val tutorialCompletedFlow: Flow<Boolean> = dao.getSetting(KEY_TUTORIAL_COMPLETED).map { value ->
        value == "true"
    }

    suspend fun isTutorialCompleted(): Boolean = dao.getSettingSync(KEY_TUTORIAL_COMPLETED) == "true"

    suspend fun saveTutorialCompleted(completed: Boolean) {
        dao.saveSetting(AppSettingEntity(KEY_TUTORIAL_COMPLETED, completed.toString()))
    }

    // --- Chapter Indexing & Reconciling ---

    /**
     * Immediately returns cached chapters from the Room database in < 5ms,
     * decorated with bookmarks and per-chapter reading progress.
     */
    suspend fun getCachedChapters(mangaName: String): List<Chapter> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val cachedEntities = dao.getChaptersForMangaSync(mangaName)
        val progressList = dao.getChapterProgressForMangaSync(mangaName)
        val progressMap = progressList.associateBy { it.chapterFileName }

        val chapters = cachedEntities.map { entity ->
            val isBookmarked = dao.isChapterBookmarkedSync(mangaName, entity.chapterFileName)
            val prog = progressMap[entity.chapterFileName]
            Chapter(
                originalFileName = entity.chapterFileName,
                displayName = entity.displayName,
                fileUri = Uri.parse(entity.documentUri),
                mangaName = entity.mangaName,
                isBookmarked = isBookmarked,
                fileSize = entity.fileSize,
                sortOrder = entity.sortOrder,
                currentPageIndex = prog?.currentPageIndex ?: 0,
                totalPages = prog?.totalPages ?: 0,
                isCompleted = prog?.isCompleted ?: false,
                lastReadTimestamp = prog?.lastReadTimestamp
            )
        }
        val tDb = System.currentTimeMillis() - t0
        Log.d(TAG, "Database lookup for '$mangaName': ${chapters.size} chapters in ${tDb}ms")
        return@withContext chapters
    }

    /**
     * Observes chapters reactively combining chapters, bookmarks, and per-chapter progress.
     */
    fun observeChaptersFlow(mangaName: String): Flow<List<Chapter>> {
        val chaptersFlow = dao.getChaptersForMangaFlow(mangaName)
        val bookmarksFlow = dao.getBookmarksForManga(mangaName)
        val progressFlow = dao.getChapterProgressForManga(mangaName)

        return combine(chaptersFlow, bookmarksFlow, progressFlow) { entities, bookmarks, progressList ->
            val bookmarkedSet = bookmarks.map { it.chapterFileName }.toHashSet()
            val progressMap = progressList.associateBy { it.chapterFileName }

            entities.map { entity ->
                val prog = progressMap[entity.chapterFileName]
                Chapter(
                    originalFileName = entity.chapterFileName,
                    displayName = entity.displayName,
                    fileUri = Uri.parse(entity.documentUri),
                    mangaName = entity.mangaName,
                    isBookmarked = bookmarkedSet.contains(entity.chapterFileName),
                    fileSize = entity.fileSize,
                    sortOrder = entity.sortOrder,
                    currentPageIndex = prog?.currentPageIndex ?: 0,
                    totalPages = prog?.totalPages ?: 0,
                    isCompleted = prog?.isCompleted ?: false,
                    lastReadTimestamp = prog?.lastReadTimestamp
                )
            }
        }
    }

    /**
     * Incremental background scan:
     * 1. Performs a single fast SAF directory query (no opening archives).
     * 2. Compares filesystem entries against existing Room records.
     * 3. Only inserts, deletes, or updates changed records.
     */
    suspend fun reconcileChaptersIncremental(
        mangaFolderUri: Uri,
        mangaName: String
    ): List<Chapter> = withContext(scanDispatcher) {
        val lock = mangaScanLocks.computeIfAbsent(mangaName) { Mutex() }
        lock.withLock {
            val tStart = System.currentTimeMillis()
            val existingChapters = dao.getChaptersForMangaSync(mangaName)
            val safFiles = StorageManager.queryChapterFilesFast(context, mangaFolderUri)

            val existingMap = existingChapters.associateBy { it.chapterFileName }
            val currentMap = safFiles.associateBy { it.name }

            val addedFiles = safFiles.filter { it.name !in existingMap }
            val deletedNames = existingChapters.filter { it.chapterFileName !in currentMap }.map { it.chapterFileName }
            val modifiedFiles = safFiles.filter { saf ->
                val existing = existingMap[saf.name]
                existing != null && (existing.fileSize != saf.size || existing.lastModified != saf.lastModified)
            }

            val hasChanges = addedFiles.isNotEmpty() || deletedNames.isNotEmpty() || modifiedFiles.isNotEmpty() || existingChapters.isEmpty()

            if (hasChanges) {
                if (deletedNames.isNotEmpty()) {
                    dao.deleteChapters(mangaName, deletedNames)
                }

                val sortedSafFiles = safFiles.sortedWith { f1, f2 ->
                    val name1 = f1.name.substringBeforeLast('.')
                    val name2 = f2.name.substringBeforeLast('.')
                    NaturalOrderComparator.compare(name1, name2)
                }

                val entitiesToSave = sortedSafFiles.mapIndexed { index, saf ->
                    ChapterEntity(
                        mangaName = mangaName,
                        chapterFileName = saf.name,
                        displayName = saf.name.substringBeforeLast('.'),
                        documentUri = saf.uri.toString(),
                        fileSize = saf.size,
                        lastModified = saf.lastModified,
                        sortOrder = index,
                        lastScannedAt = System.currentTimeMillis()
                    )
                }

                dao.insertChapters(entitiesToSave)
                dao.updateMangaChapterCount(mangaName, sortedSafFiles.size)
            }

            val tTotal = System.currentTimeMillis() - tStart
            Log.i(TAG, "Reconciled '$mangaName' chapters in ${tTotal}ms")
            return@withLock getCachedChapters(mangaName)
        }
    }

    // --- Manga Library Indexing & Covers ---

    fun observeVisibleMangaLibraryFlow(): Flow<List<Manga>> {
        return dao.getVisibleMangaFlow().map { list ->
            list.map { entity ->
                val coverFile = CoverManager.getCoverFile(context, entity.name)
                Manga(
                    name = entity.name,
                    folderUri = Uri.parse(entity.folderUri),
                    chapterCount = entity.chapterCount,
                    coverFilePath = coverFile?.absolutePath,
                    hasCustomCover = CoverManager.hasCustomCover(context, entity.name),
                    isHidden = entity.isHidden
                )
            }
        }
    }

    fun observeHiddenMangaLibraryFlow(): Flow<List<Manga>> {
        return dao.getHiddenMangaFlow().map { list ->
            list.map { entity ->
                val coverFile = CoverManager.getCoverFile(context, entity.name)
                Manga(
                    name = entity.name,
                    folderUri = Uri.parse(entity.folderUri),
                    chapterCount = entity.chapterCount,
                    coverFilePath = coverFile?.absolutePath,
                    hasCustomCover = CoverManager.hasCustomCover(context, entity.name),
                    isHidden = entity.isHidden
                )
            }
        }
    }

    fun observeMangaLibraryFlow(): Flow<List<Manga>> = observeVisibleMangaLibraryFlow()

    suspend fun getCachedVisibleMangaLibrary(): List<Manga> = withContext(Dispatchers.IO) {
        val cached = dao.getVisibleMangaSync()
        return@withContext cached.map { entity ->
            val coverFile = CoverManager.getCoverFile(context, entity.name)
            Manga(
                name = entity.name,
                folderUri = Uri.parse(entity.folderUri),
                chapterCount = entity.chapterCount,
                coverFilePath = coverFile?.absolutePath,
                hasCustomCover = CoverManager.hasCustomCover(context, entity.name),
                isHidden = entity.isHidden
            )
        }
    }

    suspend fun getCachedHiddenMangaLibrary(): List<Manga> = withContext(Dispatchers.IO) {
        val cached = dao.getHiddenMangaSync()
        return@withContext cached.map { entity ->
            val coverFile = CoverManager.getCoverFile(context, entity.name)
            Manga(
                name = entity.name,
                folderUri = Uri.parse(entity.folderUri),
                chapterCount = entity.chapterCount,
                coverFilePath = coverFile?.absolutePath,
                hasCustomCover = CoverManager.hasCustomCover(context, entity.name),
                isHidden = entity.isHidden
            )
        }
    }

    suspend fun getCachedMangaLibrary(): List<Manga> = getCachedVisibleMangaLibrary()

    suspend fun setMangaHidden(mangaName: String, isHidden: Boolean) = withContext(Dispatchers.IO) {
        dao.setMangaHidden(mangaName, isHidden)
    }

    suspend fun reconcileMangaLibrary(readerUri: Uri): List<Manga> = withContext(scanDispatcher) {
        val mangaList = StorageManager.scanMangaLibraryFast(context, readerUri)
        val existingManga = dao.getAllMangaSync().associateBy { it.name }

        val entities = mangaList.map { m ->
            val coverFile = CoverManager.getCoverFile(context, m.name)
            val existing = existingManga[m.name]
            MangaEntity(
                name = m.name,
                folderUri = m.folderUri.toString(),
                chapterCount = m.chapterCount,
                lastScannedAt = System.currentTimeMillis(),
                customCoverPath = if (CoverManager.hasCustomCover(context, m.name)) coverFile?.absolutePath else null,
                autoCoverPath = coverFile?.absolutePath ?: existing?.autoCoverPath,
                isHidden = existing?.isHidden ?: false
            )
        }
        dao.insertMangaList(entities)
        dao.deleteMissingManga(mangaList.map { it.name })

        // Return mapped manga models preserving isHidden state
        return@withContext mangaList.map { m ->
            val coverFile = CoverManager.getCoverFile(context, m.name)
            val existing = existingManga[m.name]
            m.copy(
                coverFilePath = coverFile?.absolutePath,
                hasCustomCover = CoverManager.hasCustomCover(context, m.name),
                isHidden = existing?.isHidden ?: false
            )
        }
    }

    suspend fun scanLibrary(readerUri: Uri): List<Manga> = reconcileMangaLibrary(readerUri)

    suspend fun scanChapters(mangaFolderUri: Uri, mangaName: String): List<Chapter> =
        reconcileChaptersIncremental(mangaFolderUri, mangaName)

    /**
     * Ensures an auto cover exists for the manga if not already created,
     * or forces regeneration from the first naturally sorted chapter.
     */
    suspend fun ensureAutoCover(manga: Manga, forceRegenerate: Boolean = false): File? = withContext(coverDispatcher) {
        if (!forceRegenerate) {
            val existing = CoverManager.getCoverFile(context, manga.name)
            if (existing != null) return@withContext existing
        }

        // Get first chapter
        val chapters = dao.getChaptersForMangaSync(manga.name)
        val firstChapter = chapters.minByOrNull { it.sortOrder } ?: return@withContext null

        val cover = CoverManager.getOrGenerateAutoCover(
            context = context,
            mangaName = manga.name,
            firstChapterUri = Uri.parse(firstChapter.documentUri),
            forceRegenerate = forceRegenerate
        )
        if (cover != null) {
            dao.updateMangaAutoCover(manga.name, cover.absolutePath)
        }
        return@withContext cover
    }

    suspend fun regenerateAutoCover(manga: Manga): File? =
        ensureAutoCover(manga, forceRegenerate = true)

    suspend fun setCustomCover(mangaName: String, imageUri: Uri): File? = withContext(coverDispatcher) {
        val file = CoverManager.setCustomCover(context, mangaName, imageUri)
        if (file != null) {
            dao.updateMangaCustomCover(mangaName, file.absolutePath)
        }
        return@withContext file
    }

    suspend fun resetCustomCover(mangaName: String): Boolean = withContext(coverDispatcher) {
        val reset = CoverManager.resetCustomCover(context, mangaName)
        if (reset) {
            dao.updateMangaCustomCover(mangaName, null)
        }
        return@withContext reset
    }

    // --- Archive Pages ---
    suspend fun loadChapterPages(
        chapterUri: Uri,
        mangaName: String,
        chapterFileName: String,
        forceReextract: Boolean = false
    ): List<File> {
        return ArchiveManager.getOrExtractChapterPages(
            context = context,
            chapterUri = chapterUri,
            mangaName = mangaName,
            chapterFileName = chapterFileName,
            forceReextract = forceReextract
        )
    }

    // --- Independent Per-Chapter Reading Progress & History ---
    val allChapterHistory: Flow<List<ChapterReadingProgressEntity>> = dao.getAllChapterProgress()

    suspend fun getAllChapterHistorySync(): List<ChapterReadingProgressEntity> =
        dao.getAllChapterProgressSync()

    suspend fun getChapterProgress(mangaName: String, chapterFileName: String): ChapterReadingProgressEntity? =
        dao.getChapterProgressSync(mangaName, chapterFileName)

    fun observeChapterProgress(mangaName: String, chapterFileName: String): Flow<ChapterReadingProgressEntity?> =
        dao.getChapterProgress(mangaName, chapterFileName)

    /**
     * Saves progress for an individual chapter and updates the manga-level "Continue Reading" pointer.
     */
    suspend fun saveChapterProgress(
        mangaName: String,
        chapterFileName: String,
        pageIndex: Int,
        totalPages: Int = 0,
        isCompleted: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dao.getChapterProgressSync(mangaName, chapterFileName)
        val finalTotalPages = if (totalPages > 0) totalPages else (existing?.totalPages ?: 0)
        val finalCompleted = isCompleted || (finalTotalPages > 0 && pageIndex >= finalTotalPages - 1)

        val chapterEntity = ChapterReadingProgressEntity(
            mangaName = mangaName,
            chapterFileName = chapterFileName,
            currentPageIndex = pageIndex,
            totalPages = finalTotalPages,
            isCompleted = finalCompleted,
            lastReadTimestamp = now,
            updatedAt = now
        )
        dao.saveChapterProgress(chapterEntity)

        // Also update manga-level continue reading pointer
        dao.saveReadingProgress(
            ReadingProgressEntity(
                mangaName = mangaName,
                chapterFileName = chapterFileName,
                pageIndex = pageIndex,
                timestamp = now
            )
        )
    }

    suspend fun markChapterCompleted(
        mangaName: String,
        chapterFileName: String,
        totalPages: Int = 0
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dao.getChapterProgressSync(mangaName, chapterFileName)
        val finalTotalPages = if (totalPages > 0) totalPages else (existing?.totalPages ?: 0)
        val targetPage = if (finalTotalPages > 0) finalTotalPages - 1 else (existing?.currentPageIndex ?: 0)

        val entity = ChapterReadingProgressEntity(
            mangaName = mangaName,
            chapterFileName = chapterFileName,
            currentPageIndex = targetPage,
            totalPages = finalTotalPages,
            isCompleted = true,
            lastReadTimestamp = now,
            updatedAt = now
        )
        dao.saveChapterProgress(entity)

        dao.saveReadingProgress(
            ReadingProgressEntity(
                mangaName = mangaName,
                chapterFileName = chapterFileName,
                pageIndex = targetPage,
                timestamp = now
            )
        )
    }

    suspend fun markChapterUnread(
        mangaName: String,
        chapterFileName: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dao.getChapterProgressSync(mangaName, chapterFileName)
        val entity = ChapterReadingProgressEntity(
            mangaName = mangaName,
            chapterFileName = chapterFileName,
            currentPageIndex = 0,
            totalPages = existing?.totalPages ?: 0,
            isCompleted = false,
            lastReadTimestamp = now,
            updatedAt = now
        )
        dao.saveChapterProgress(entity)
    }

    suspend fun deleteHistoryItem(mangaName: String, chapterFileName: String) = withContext(Dispatchers.IO) {
        dao.deleteChapterProgress(mangaName, chapterFileName)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearAllChapterProgress()
    }

    // --- Continue Reading (Manga Level) ---
    val allReadingProgress: Flow<List<ReadingProgressEntity>> = dao.getAllReadingProgress()

    suspend fun getProgressForManga(mangaName: String): ReadingProgressEntity? =
        dao.getProgressForMangaSync(mangaName)

    fun observeProgressForManga(mangaName: String): Flow<ReadingProgressEntity?> =
        dao.getProgressForManga(mangaName)

    // Legacy helper for backward compatibility
    suspend fun saveProgress(mangaName: String, chapterFileName: String, pageIndex: Int) {
        saveChapterProgress(mangaName, chapterFileName, pageIndex, 0, false)
    }

    // --- Bookmarks ---
    val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    fun isChapterBookmarked(mangaName: String, chapterFileName: String): Flow<Boolean> =
        dao.isChapterBookmarked(mangaName, chapterFileName)

    suspend fun toggleBookmark(mangaName: String, chapterFileName: String): Boolean {
        val currentlyBookmarked = dao.isChapterBookmarkedSync(mangaName, chapterFileName)
        if (currentlyBookmarked) {
            dao.deleteBookmark(mangaName, chapterFileName)
            return false
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    mangaName = mangaName,
                    chapterFileName = chapterFileName,
                    timestamp = System.currentTimeMillis()
                )
            )
            return true
        }
    }

    suspend fun removeBookmark(mangaName: String, chapterFileName: String) {
        dao.deleteBookmark(mangaName, chapterFileName)
    }
}
