package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ReaderDatabase
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.data.repository.ReaderRepository
import com.example.model.Chapter
import com.example.model.HoldScrollSpeed
import com.example.model.Manga
import com.example.model.ReaderItem
import com.example.model.ReadingDirection
import com.example.model.ReadingMode
import com.example.model.TapAction
import com.example.ui.components.AppTab
import com.example.util.SecurityManager
import com.example.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ScreenState {
    WELCOME,
    MAIN_TABS,
    CHAPTER_LIST,
    READER,
    HIDDEN_LIBRARY
}

data class ReaderUiState(
    val screen: ScreenState = ScreenState.WELCOME,
    val currentTab: AppTab = AppTab.LIBRARY,
    val readerFolderUri: String? = null,
    val isScanning: Boolean = false,
    val isBackgroundReconciling: Boolean = false,
    val mangaList: List<Manga> = emptyList(),
    val hiddenMangaList: List<Manga> = emptyList(),
    val isHiddenLibraryUnlocked: Boolean = false,
    val selectedManga: Manga? = null,
    val chapters: List<Chapter> = emptyList(),
    val chapterSearchQuery: String = "",
    // Reader active state
    val activeManga: Manga? = null,
    val activeChapter: Chapter? = null,
    val activePages: List<File> = emptyList(),
    val readerItems: List<ReaderItem> = emptyList(),
    val isLoadingPages: Boolean = false,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val readingMode: ReadingMode = ReadingMode.SINGLE_PAGE,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val holdScrollSpeed: HoldScrollSpeed = HoldScrollSpeed.NORMAL,
    val isCurrentChapterBookmarked: Boolean = false,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
    val nextChapter: Chapter? = null,
    // Settings & dialogs
    val defaultReadingMode: ReadingMode = ReadingMode.SINGLE_PAGE,
    val defaultReadingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val chapterTransitionsEnabled: Boolean = true,
    val autoLoadNextChapter: Boolean = true,
    val chapterTransitionStyle: String = "SHOW_TITLE", // "SHOW_TITLE" or "MINIMAL"
    // Customizable gesture settings
    val pagedRightTapAction: TapAction = TapAction.NEXT_PAGE,
    val pagedLeftTapAction: TapAction = TapAction.PREVIOUS_PAGE,
    val reversePageNavigation: Boolean = false,
    val holdScrollSpeedPx: Int = 650,
    val holdActivationDelayMs: Int = 300,
    val reverseHoldScrollDirection: Boolean = false,
    val verticalTapScrollPercent: Int = 50,
    val showSettings: Boolean = false,
    val showPinSetupDialog: Boolean = false,
    val showPinAuthDialog: Boolean = false,
    val showChangePinDialog: Boolean = false,
    val showTutorialDialog: Boolean = false,
    val errorMessage: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReaderRepository(
        context = application,
        database = ReaderDatabase.getInstance(application)
    )

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var activeChapterScanJob: Job? = null
    private var debouncedSaveJob: Job? = null
    private var prefetchJob: Job? = null

    // Set of chapter file names currently attached to continuous stream
    private val attachedChapterNames = mutableSetOf<String>()

    // Pending state for debounced progress persistence
    private var pendingMangaName: String? = null
    private var pendingChapterName: String? = null
    private var pendingPageIndex: Int = 0
    private var pendingTotalPages: Int = 0

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReadingProgress: StateFlow<List<ReadingProgressEntity>> = repository.allReadingProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChapterHistory: StateFlow<List<ChapterReadingProgressEntity>> = repository.allChapterHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val isCompleted = repository.isTutorialCompleted()
            if (!isCompleted) {
                _uiState.value = _uiState.value.copy(showTutorialDialog = true)
            }
        }

        viewModelScope.launch {
            val savedFolder = repository.getSavedReaderFolderUri()
            if (savedFolder != null && StorageManager.hasPermission(getApplication(), Uri.parse(savedFolder))) {
                // Instantly load cached visible manga list (< 5ms)
                val cachedVisible = repository.getCachedVisibleMangaLibrary()
                val cachedHidden = repository.getCachedHiddenMangaLibrary()
                _uiState.value = _uiState.value.copy(
                    readerFolderUri = savedFolder,
                    mangaList = cachedVisible,
                    hiddenMangaList = cachedHidden,
                    screen = ScreenState.MAIN_TABS
                )
                // Trigger covers check in background
                triggerCoversGeneration(cachedVisible + cachedHidden)
                scanLibraryInternal(Uri.parse(savedFolder))
            } else {
                _uiState.value = _uiState.value.copy(screen = ScreenState.WELCOME)
            }
        }

        viewModelScope.launch {
            repository.observeVisibleMangaLibraryFlow().collect { visible ->
                _uiState.value = _uiState.value.copy(mangaList = visible)
            }
        }

        viewModelScope.launch {
            repository.observeHiddenMangaLibraryFlow().collect { hidden ->
                _uiState.value = _uiState.value.copy(hiddenMangaList = hidden)
            }
        }

        viewModelScope.launch {
            repository.defaultReadingModeFlow.collect { mode ->
                _uiState.value = _uiState.value.copy(defaultReadingMode = mode)
            }
        }

        viewModelScope.launch {
            repository.defaultReadingDirectionFlow.collect { dir ->
                _uiState.value = _uiState.value.copy(defaultReadingDirection = dir)
            }
        }

        viewModelScope.launch {
            repository.chapterTransitionsEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(chapterTransitionsEnabled = enabled)
            }
        }

        viewModelScope.launch {
            repository.autoLoadNextChapterFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(autoLoadNextChapter = enabled)
            }
        }

        viewModelScope.launch {
            repository.chapterTransitionStyleFlow.collect { style ->
                _uiState.value = _uiState.value.copy(chapterTransitionStyle = style)
            }
        }

        viewModelScope.launch {
            repository.holdScrollSpeedFlow.collect { speed ->
                _uiState.value = _uiState.value.copy(holdScrollSpeed = speed)
            }
        }

        viewModelScope.launch {
            repository.pagedRightTapActionFlow.collect { action ->
                _uiState.value = _uiState.value.copy(pagedRightTapAction = action)
            }
        }

        viewModelScope.launch {
            repository.pagedLeftTapActionFlow.collect { action ->
                _uiState.value = _uiState.value.copy(pagedLeftTapAction = action)
            }
        }

        viewModelScope.launch {
            repository.reversePageNavigationFlow.collect { reverse ->
                _uiState.value = _uiState.value.copy(reversePageNavigation = reverse)
            }
        }

        viewModelScope.launch {
            repository.holdScrollSpeedPxFlow.collect { speedPx ->
                _uiState.value = _uiState.value.copy(holdScrollSpeedPx = speedPx)
            }
        }

        viewModelScope.launch {
            repository.holdActivationDelayMsFlow.collect { delayMs ->
                _uiState.value = _uiState.value.copy(holdActivationDelayMs = delayMs)
            }
        }

        viewModelScope.launch {
            repository.reverseHoldScrollDirectionFlow.collect { reverse ->
                _uiState.value = _uiState.value.copy(reverseHoldScrollDirection = reverse)
            }
        }

        viewModelScope.launch {
            repository.verticalTapScrollPercentFlow.collect { percent ->
                _uiState.value = _uiState.value.copy(verticalTapScrollPercent = percent)
            }
        }
    }

    private fun triggerCoversGeneration(mangaList: List<Manga>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (manga in mangaList) {
                if (manga.coverFilePath == null) {
                    val cover = repository.ensureAutoCover(manga)
                    if (cover != null) {
                        // Update manga item cover path in state
                        val updated = _uiState.value.mangaList.map {
                            if (it.name == manga.name) it.copy(coverFilePath = cover.absolutePath) else it
                        }
                        val updatedHidden = _uiState.value.hiddenMangaList.map {
                            if (it.name == manga.name) it.copy(coverFilePath = cover.absolutePath) else it
                        }
                        _uiState.value = _uiState.value.copy(
                            mangaList = updated,
                            hiddenMangaList = updatedHidden
                        )
                    }
                }
            }
        }
    }

    fun onFolderSelected(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            val readerFolderUri = StorageManager.setupReaderFolder(getApplication(), treeUri)
            if (readerFolderUri != null) {
                repository.saveReaderFolderUri(readerFolderUri.toString())
                val isTutorialDone = repository.isTutorialCompleted()
                _uiState.value = _uiState.value.copy(
                    readerFolderUri = readerFolderUri.toString(),
                    screen = ScreenState.MAIN_TABS,
                    showTutorialDialog = !isTutorialDone,
                    isScanning = false
                )
                scanLibraryInternal(readerFolderUri)
            } else {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Failed to access or create Reader folder. Please try selecting a location again."
                )
            }
        }
    }

    fun rescanLibrary() {
        val uriStr = _uiState.value.readerFolderUri ?: return
        scanLibraryInternal(Uri.parse(uriStr))
        val selected = _uiState.value.selectedManga
        if (selected != null) {
            openManga(selected)
        }
    }

    private fun scanLibraryInternal(folderUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                val manga = repository.scanLibrary(folderUri)
                val visible = manga.filter { !it.isHidden }
                val hidden = manga.filter { it.isHidden }
                _uiState.value = _uiState.value.copy(
                    mangaList = visible,
                    hiddenMangaList = hidden,
                    isScanning = false
                )
                triggerCoversGeneration(manga)
            } catch (e: Exception) {
                Log.e("ReaderVM", "Error scanning library: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Error scanning library: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, errorMessage = null)
    }

    fun setChapterSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(chapterSearchQuery = query)
    }

    fun openManga(manga: Manga) {
        activeChapterScanJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedManga = manga,
            chapterSearchQuery = "",
            screen = ScreenState.CHAPTER_LIST,
            errorMessage = null
        )

        viewModelScope.launch {
            // Step 1: Immediately retrieve cached chapters (< 5ms)
            val cached = repository.getCachedChapters(manga.name)
            if (cached.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    chapters = cached,
                    isScanning = false,
                    isBackgroundReconciling = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    chapters = emptyList(),
                    isScanning = true,
                    isBackgroundReconciling = false
                )
            }

            // Step 2: Incremental background scan
            activeChapterScanJob = viewModelScope.launch {
                try {
                    val reconciled = repository.reconcileChaptersIncremental(manga.folderUri, manga.name)
                    if (_uiState.value.selectedManga?.name == manga.name) {
                        _uiState.value = _uiState.value.copy(
                            chapters = reconciled,
                            isScanning = false,
                            isBackgroundReconciling = false
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ReaderVM", "Error reconciling chapters for ${manga.name}: ${e.message}", e)
                    if (_uiState.value.selectedManga?.name == manga.name) {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            isBackgroundReconciling = false,
                            errorMessage = if (_uiState.value.chapters.isEmpty()) "Error reading chapters for ${manga.name}" else null
                        )
                    }
                }
            }
        }
    }

    fun closeChapterList() {
        activeChapterScanJob?.cancel()
        activeChapterScanJob = null
        val targetScreen = if (_uiState.value.selectedManga?.isHidden == true && _uiState.value.isHiddenLibraryUnlocked) {
            ScreenState.HIDDEN_LIBRARY
        } else {
            ScreenState.MAIN_TABS
        }
        _uiState.value = _uiState.value.copy(
            selectedManga = null,
            chapters = emptyList(),
            isScanning = false,
            isBackgroundReconciling = false,
            chapterSearchQuery = "",
            screen = targetScreen
        )
    }

    fun openChapter(chapter: Chapter, initialPage: Int = -1, mangaOverride: Manga? = null) {
        prefetchJob?.cancel()
        attachedChapterNames.clear()

        val manga = mangaOverride ?: _uiState.value.selectedManga ?: Manga(
            name = chapter.mangaName,
            folderUri = chapter.fileUri,
            chapterCount = 1
        )

        viewModelScope.launch {
            var currentChapterList = _uiState.value.chapters
            if (currentChapterList.isEmpty() || currentChapterList.firstOrNull()?.mangaName != manga.name) {
                currentChapterList = repository.getCachedChapters(manga.name).ifEmpty {
                    repository.scanChapters(manga.folderUri, manga.name)
                }
            }

            val currentIndex = currentChapterList.indexOfFirst { it.originalFileName == chapter.originalFileName }
            val hasPrev = currentIndex > 0
            val hasNext = currentIndex in 0 until currentChapterList.size - 1
            val nextChapterObj = if (hasNext) currentChapterList[currentIndex + 1] else null

            // Resolve initial page: if initialPage >= 0, use it; else use chapter.currentPageIndex
            val savedProgress = repository.getChapterProgress(manga.name, chapter.originalFileName)
            val resolvedPage = if (initialPage >= 0) {
                initialPage
            } else {
                savedProgress?.currentPageIndex ?: chapter.currentPageIndex
            }

            val activeReadingMode = _uiState.value.defaultReadingMode

            _uiState.value = _uiState.value.copy(
                screen = ScreenState.READER,
                activeManga = manga,
                activeChapter = chapter,
                chapters = currentChapterList,
                isLoadingPages = true,
                currentPageIndex = resolvedPage,
                totalPages = savedProgress?.totalPages ?: chapter.totalPages,
                readingMode = activeReadingMode,
                readingDirection = _uiState.value.defaultReadingDirection,
                hasPreviousChapter = hasPrev,
                hasNextChapter = hasNext,
                nextChapter = nextChapterObj,
                readerItems = emptyList(),
                errorMessage = null
            )

            try {
                val pages = repository.loadChapterPages(chapter.fileUri, manga.name, chapter.originalFileName)
                val total = pages.size
                val safePage = resolvedPage.coerceIn(0, (total - 1).coerceAtLeast(0))

                attachedChapterNames.add(chapter.originalFileName)

                val initialItems = pages.mapIndexed { idx, file ->
                    ReaderItem.Page(
                        chapter = chapter,
                        pageIndex = idx,
                        totalPages = total,
                        file = file
                    )
                }

                _uiState.value = _uiState.value.copy(
                    activePages = pages,
                    readerItems = initialItems,
                    isLoadingPages = false,
                    currentPageIndex = safePage,
                    totalPages = total
                )

                // Track and save progress immediately on open
                pendingMangaName = manga.name
                pendingChapterName = chapter.originalFileName
                pendingPageIndex = safePage
                pendingTotalPages = total
                repository.saveChapterProgress(
                    mangaName = manga.name,
                    chapterFileName = chapter.originalFileName,
                    pageIndex = safePage,
                    totalPages = total,
                    isCompleted = (total > 0 && safePage >= total - 1)
                )

                // Collect bookmark state
                val isBookmarkedFlow = repository.isChapterBookmarked(manga.name, chapter.originalFileName)
                launch {
                    isBookmarkedFlow.collect { bookmarked ->
                        _uiState.value = _uiState.value.copy(isCurrentChapterBookmarked = bookmarked)
                    }
                }

                // In vertical continuous mode, prefetch next chapter if auto-load is enabled
                if (activeReadingMode == ReadingMode.VERTICAL_CONTINUOUS &&
                    _uiState.value.autoLoadNextChapter &&
                    nextChapterObj != null
                ) {
                    prefetchAndAppendNextChapter(manga, chapter, nextChapterObj)
                }

            } catch (e: Exception) {
                Log.e("ReaderVM", "Error loading chapter pages: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingPages = false,
                    errorMessage = "Failed to load chapter: ${e.localizedMessage ?: "Corrupted or unsupported archive"}"
                )
            }
        }
    }

    /**
     * Asynchronously extracts and appends the next chapter in vertical continuous reading mode.
     */
    private fun prefetchAndAppendNextChapter(
        manga: Manga,
        prevChapter: Chapter,
        nextChapter: Chapter
    ) {
        if (attachedChapterNames.contains(nextChapter.originalFileName)) return
        attachedChapterNames.add(nextChapter.originalFileName)

        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i("ReaderVM", "Prefetching next chapter '${nextChapter.displayName}' for continuous stream")
                val nextPages = repository.loadChapterPages(
                    chapterUri = nextChapter.fileUri,
                    mangaName = manga.name,
                    chapterFileName = nextChapter.originalFileName
                )

                if (nextPages.isEmpty()) return@launch

                val newItems = mutableListOf<ReaderItem>()
                if (_uiState.value.chapterTransitionsEnabled) {
                    newItems.add(
                        ReaderItem.ChapterSeparator(
                            previousChapter = prevChapter,
                            nextChapter = nextChapter
                        )
                    )
                }

                val nextTotal = nextPages.size
                nextPages.forEachIndexed { idx, file ->
                    newItems.add(
                        ReaderItem.Page(
                            chapter = nextChapter,
                            pageIndex = idx,
                            totalPages = nextTotal,
                            file = file
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    val currentItems = _uiState.value.readerItems
                    _uiState.value = _uiState.value.copy(
                        readerItems = currentItems + newItems
                    )
                }
            } catch (e: Exception) {
                Log.w("ReaderVM", "Could not prefetch next chapter ${nextChapter.displayName}: ${e.message}")
            }
        }
    }

    /**
     * Called in vertical continuous mode when a ReaderItem becomes visible on screen.
     * Manages seamless chapter transition, independent progress tracking, and further look-ahead.
     */
    fun onContinuousItemVisible(item: ReaderItem, visibleIndex: Int) {
        if (item !is ReaderItem.Page) return

        val currentActive = _uiState.value.activeChapter
        val manga = _uiState.value.activeManga ?: return

        // Check if user transitioned into a different chapter
        if (currentActive != null && currentActive.originalFileName != item.chapter.originalFileName) {
            // Previous chapter reached final page and transitioned: mark it completed
            viewModelScope.launch(Dispatchers.IO) {
                repository.markChapterCompleted(
                    mangaName = manga.name,
                    chapterFileName = currentActive.originalFileName,
                    totalPages = currentActive.totalPages
                )
            }

            // Update active chapter and recalculate next/previous pointers
            val allChapters = _uiState.value.chapters
            val idx = allChapters.indexOfFirst { it.originalFileName == item.chapter.originalFileName }
            val hasPrev = idx > 0
            val hasNext = idx in 0 until allChapters.size - 1
            val nextObj = if (hasNext) allChapters[idx + 1] else null

            _uiState.value = _uiState.value.copy(
                activeChapter = item.chapter,
                totalPages = item.totalPages,
                currentPageIndex = item.pageIndex,
                hasPreviousChapter = hasPrev,
                hasNextChapter = hasNext,
                nextChapter = nextObj
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentPageIndex = item.pageIndex,
                totalPages = item.totalPages
            )
        }

        // Independent chapter progress recording
        pendingMangaName = manga.name
        pendingChapterName = item.chapter.originalFileName
        pendingPageIndex = item.pageIndex
        pendingTotalPages = item.totalPages

        debouncedSaveJob?.cancel()
        debouncedSaveJob = viewModelScope.launch {
            delay(700L)
            flushPendingReadingProgress()
        }

        // Look-ahead check: if nearing end of stream and next chapter not yet appended
        val totalItems = _uiState.value.readerItems.size
        if (visibleIndex >= totalItems - 5 && _uiState.value.autoLoadNextChapter) {
            val allChapters = _uiState.value.chapters
            val currentIdx = allChapters.indexOfFirst { it.originalFileName == item.chapter.originalFileName }
            if (currentIdx in 0 until allChapters.size - 1) {
                val nextChapter = allChapters[currentIdx + 1]
                if (!attachedChapterNames.contains(nextChapter.originalFileName)) {
                    prefetchAndAppendNextChapter(manga, item.chapter, nextChapter)
                }
            }
        }
    }

    /**
     * Debounced reading progress update (500–1000ms).
     * Used primarily in single-page mode.
     */
    fun updateReadingPage(pageIndex: Int) {
        val manga = _uiState.value.activeManga ?: return
        val chapter = _uiState.value.activeChapter ?: return
        val total = _uiState.value.totalPages

        if (pageIndex == _uiState.value.currentPageIndex) return

        _uiState.value = _uiState.value.copy(currentPageIndex = pageIndex)

        pendingMangaName = manga.name
        pendingChapterName = chapter.originalFileName
        pendingPageIndex = pageIndex
        pendingTotalPages = total

        debouncedSaveJob?.cancel()
        debouncedSaveJob = viewModelScope.launch {
            delay(700L) // 700ms debounce
            flushPendingReadingProgress()
        }
    }

    /**
     * Immediately flushes any pending reading progress to Room database.
     */
    fun flushPendingReadingProgress() {
        val mangaName = pendingMangaName ?: return
        val chapterName = pendingChapterName ?: return
        val page = pendingPageIndex
        val total = pendingTotalPages

        val isCompleted = total > 0 && page >= total - 1

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveChapterProgress(
                mangaName = mangaName,
                chapterFileName = chapterName,
                pageIndex = page,
                totalPages = total,
                isCompleted = isCompleted
            )
        }
    }

    fun closeReader() {
        debouncedSaveJob?.cancel()
        prefetchJob?.cancel()
        flushPendingReadingProgress()

        val manga = _uiState.value.selectedManga
        if (manga != null) {
            // Refresh chapter list with updated progress
            viewModelScope.launch {
                val updated = repository.getCachedChapters(manga.name)
                _uiState.value = _uiState.value.copy(chapters = updated)
            }
        }

        _uiState.value = _uiState.value.copy(
            screen = if (_uiState.value.selectedManga != null) ScreenState.CHAPTER_LIST else ScreenState.MAIN_TABS,
            activeChapter = null,
            activePages = emptyList(),
            readerItems = emptyList(),
            errorMessage = null
        )
    }

    fun nextChapter() {
        debouncedSaveJob?.cancel()
        flushPendingReadingProgress()

        val currentChapter = _uiState.value.activeChapter ?: return
        val currentList = _uiState.value.chapters
        val idx = currentList.indexOfFirst { it.originalFileName == currentChapter.originalFileName }
        if (idx != -1 && idx < currentList.size - 1) {
            val next = currentList[idx + 1]
            openChapter(next, initialPage = 0)
        }
    }

    fun previousChapter() {
        debouncedSaveJob?.cancel()
        flushPendingReadingProgress()

        val currentChapter = _uiState.value.activeChapter ?: return
        val currentList = _uiState.value.chapters
        val idx = currentList.indexOfFirst { it.originalFileName == currentChapter.originalFileName }
        if (idx > 0) {
            val prev = currentList[idx - 1]
            openChapter(prev, initialPage = 0)
        }
    }

    // --- Chapter Actions: Mark as Completed / Unread ---

    fun markChapterCompleted(chapter: Chapter) {
        viewModelScope.launch {
            repository.markChapterCompleted(chapter.mangaName, chapter.originalFileName, chapter.totalPages)
            val manga = _uiState.value.selectedManga ?: return@launch
            val updated = repository.getCachedChapters(manga.name)
            _uiState.value = _uiState.value.copy(chapters = updated)
        }
    }

    fun markChapterUnread(chapter: Chapter) {
        viewModelScope.launch {
            repository.markChapterUnread(chapter.mangaName, chapter.originalFileName)
            val manga = _uiState.value.selectedManga ?: return@launch
            val updated = repository.getCachedChapters(manga.name)
            _uiState.value = _uiState.value.copy(chapters = updated)
        }
    }

    // --- Bookmarks ---

    fun toggleCurrentChapterBookmark() {
        val manga = _uiState.value.activeManga ?: return
        val chapter = _uiState.value.activeChapter ?: return
        viewModelScope.launch {
            val bookmarked = repository.toggleBookmark(manga.name, chapter.originalFileName)
            _uiState.value = _uiState.value.copy(isCurrentChapterBookmarked = bookmarked)
        }
    }

    fun toggleChapterBookmarkFromList(chapter: Chapter) {
        viewModelScope.launch {
            repository.toggleBookmark(chapter.mangaName, chapter.originalFileName)
            val manga = _uiState.value.selectedManga ?: return@launch
            val updated = repository.getCachedChapters(manga.name)
            _uiState.value = _uiState.value.copy(chapters = updated)
        }
    }

    fun removeBookmark(mangaName: String, chapterFileName: String) {
        viewModelScope.launch {
            repository.removeBookmark(mangaName, chapterFileName)
        }
    }

    // --- Custom & Automatic Covers ---

    fun setCustomCover(manga: Manga, imageUri: Uri) {
        viewModelScope.launch {
            val newFile = repository.setCustomCover(manga.name, imageUri)
            if (newFile != null) {
                val updatedList = _uiState.value.mangaList.map {
                    if (it.name == manga.name) it.copy(coverFilePath = newFile.absolutePath, hasCustomCover = true) else it
                }
                val updatedHidden = _uiState.value.hiddenMangaList.map {
                    if (it.name == manga.name) it.copy(coverFilePath = newFile.absolutePath, hasCustomCover = true) else it
                }
                _uiState.value = _uiState.value.copy(
                    mangaList = updatedList,
                    hiddenMangaList = updatedHidden
                )
            }
        }
    }

    fun resetCustomCover(manga: Manga) {
        viewModelScope.launch {
            repository.resetCustomCover(manga.name)
            val autoFile = repository.ensureAutoCover(manga)
            val updatedList = _uiState.value.mangaList.map {
                if (it.name == manga.name) it.copy(coverFilePath = autoFile?.absolutePath, hasCustomCover = false) else it
            }
            val updatedHidden = _uiState.value.hiddenMangaList.map {
                if (it.name == manga.name) it.copy(coverFilePath = autoFile?.absolutePath, hasCustomCover = false) else it
            }
            _uiState.value = _uiState.value.copy(
                mangaList = updatedList,
                hiddenMangaList = updatedHidden
            )
        }
    }

    fun regenerateAutoCover(manga: Manga) {
        viewModelScope.launch {
            val newFile = repository.regenerateAutoCover(manga)
            if (newFile != null) {
                val updatedList = _uiState.value.mangaList.map {
                    if (it.name == manga.name) it.copy(coverFilePath = newFile.absolutePath) else it
                }
                val updatedHidden = _uiState.value.hiddenMangaList.map {
                    if (it.name == manga.name) it.copy(coverFilePath = newFile.absolutePath) else it
                }
                _uiState.value = _uiState.value.copy(
                    mangaList = updatedList,
                    hiddenMangaList = updatedHidden
                )
            }
        }
    }

    // --- Hidden Manga & PIN Protection ---

    fun hideManga(manga: Manga) {
        viewModelScope.launch {
            repository.setMangaHidden(manga.name, true)
        }
    }

    fun unhideManga(manga: Manga) {
        viewModelScope.launch {
            repository.setMangaHidden(manga.name, false)
        }
    }

    fun onActivateHiddenLibrary() {
        val isConfigured = SecurityManager.isPinConfigured(getApplication())
        if (isConfigured) {
            _uiState.value = _uiState.value.copy(
                showPinAuthDialog = true,
                showSettings = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                showPinSetupDialog = true,
                showSettings = false
            )
        }
    }

    fun setupPin(pin: String) {
        val success = SecurityManager.setupPin(getApplication(), pin)
        if (success) {
            _uiState.value = _uiState.value.copy(
                showPinSetupDialog = false,
                isHiddenLibraryUnlocked = true,
                screen = ScreenState.HIDDEN_LIBRARY
            )
        }
    }

    fun verifyPin(pin: String): Boolean {
        return SecurityManager.verifyPin(getApplication(), pin)
    }

    fun onPinAuthSuccess() {
        _uiState.value = _uiState.value.copy(
            showPinAuthDialog = false,
            isHiddenLibraryUnlocked = true,
            screen = ScreenState.HIDDEN_LIBRARY
        )
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        return SecurityManager.changePin(getApplication(), oldPin, newPin)
    }

    fun showPinSetupDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPinSetupDialog = show)
    }

    fun showPinAuthDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPinAuthDialog = show)
    }

    fun showChangePinDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChangePinDialog = show)
    }

    fun lockHiddenLibrary() {
        _uiState.value = _uiState.value.copy(
            isHiddenLibraryUnlocked = false,
            screen = ScreenState.MAIN_TABS
        )
    }

    // --- Tutorial ---

    fun showTutorial(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTutorialDialog = show)
    }

    fun completeTutorial() {
        viewModelScope.launch {
            repository.saveTutorialCompleted(true)
            _uiState.value = _uiState.value.copy(showTutorialDialog = false)
        }
    }

    fun skipTutorial() {
        viewModelScope.launch {
            repository.saveTutorialCompleted(true)
            _uiState.value = _uiState.value.copy(showTutorialDialog = false)
        }
    }

    // --- History & Continue Reading Navigation ---

    fun resumeMangaProgress(progress: ReadingProgressEntity) {
        viewModelScope.launch {
            val folderUriStr = _uiState.value.readerFolderUri ?: return@launch
            val mangaList = if (_uiState.value.mangaList.isEmpty()) {
                repository.getCachedMangaLibrary().ifEmpty {
                    repository.scanLibrary(Uri.parse(folderUriStr))
                }
            } else {
                _uiState.value.mangaList
            }

            val targetManga = mangaList.find { it.name == progress.mangaName }
            if (targetManga == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Manga folder '${progress.mangaName}' not found in library."
                )
                return@launch
            }

            val chapters = repository.getCachedChapters(targetManga.name).ifEmpty {
                repository.scanChapters(targetManga.folderUri, targetManga.name)
            }
            val targetChapter = chapters.find { it.originalFileName == progress.chapterFileName }
            if (targetChapter == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Chapter '${progress.chapterFileName}' not found."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(selectedManga = targetManga, chapters = chapters)
            openChapter(targetChapter, initialPage = progress.pageIndex, mangaOverride = targetManga)
        }
    }

    fun openHistoryItem(item: ChapterReadingProgressEntity) {
        viewModelScope.launch {
            val folderUriStr = _uiState.value.readerFolderUri ?: return@launch
            val mangaList = if (_uiState.value.mangaList.isEmpty()) {
                repository.getCachedMangaLibrary().ifEmpty {
                    repository.scanLibrary(Uri.parse(folderUriStr))
                }
            } else {
                _uiState.value.mangaList
            }

            val targetManga = mangaList.find { it.name == item.mangaName }
            if (targetManga == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Manga folder '${item.mangaName}' not found in library."
                )
                return@launch
            }

            val chapters = repository.getCachedChapters(targetManga.name).ifEmpty {
                repository.scanChapters(targetManga.folderUri, targetManga.name)
            }
            val targetChapter = chapters.find { it.originalFileName == item.chapterFileName }
            if (targetChapter == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Chapter '${item.chapterFileName}' not found."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(selectedManga = targetManga, chapters = chapters)
            openChapter(targetChapter, initialPage = item.currentPageIndex, mangaOverride = targetManga)
        }
    }

    fun deleteHistoryItem(item: ChapterReadingProgressEntity) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item.mangaName, item.chapterFileName)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun openBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            val folderUriStr = _uiState.value.readerFolderUri ?: return@launch
            val mangaList = if (_uiState.value.mangaList.isEmpty()) {
                repository.getCachedMangaLibrary().ifEmpty {
                    repository.scanLibrary(Uri.parse(folderUriStr))
                }
            } else {
                _uiState.value.mangaList
            }

            val targetManga = mangaList.find { it.name == bookmark.mangaName }
            if (targetManga == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Manga folder '${bookmark.mangaName}' not found in library."
                )
                return@launch
            }

            val chapters = repository.getCachedChapters(targetManga.name).ifEmpty {
                repository.scanChapters(targetManga.folderUri, targetManga.name)
            }
            val targetChapter = chapters.find { it.originalFileName == bookmark.chapterFileName }
            if (targetChapter == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Chapter '${bookmark.chapterFileName}' not found."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(selectedManga = targetManga, chapters = chapters)
            openChapter(targetChapter, initialPage = 0, mangaOverride = targetManga)
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.value = _uiState.value.copy(readingMode = mode)
        val activeChapter = _uiState.value.activeChapter ?: return
        val activeManga = _uiState.value.activeManga ?: return
        val next = _uiState.value.nextChapter
        if (mode == ReadingMode.VERTICAL_CONTINUOUS && _uiState.value.autoLoadNextChapter && next != null) {
            prefetchAndAppendNextChapter(activeManga, activeChapter, next)
        }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        _uiState.value = _uiState.value.copy(readingDirection = direction)
    }

    fun setDefaultReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            repository.saveDefaultReadingMode(mode)
        }
    }

    fun setDefaultReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            repository.saveDefaultReadingDirection(direction)
        }
    }

    fun setChapterTransitionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveChapterTransitionsEnabled(enabled)
            _uiState.value = _uiState.value.copy(chapterTransitionsEnabled = enabled)
        }
    }

    fun setAutoLoadNextChapter(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoLoadNextChapter(enabled)
            _uiState.value = _uiState.value.copy(autoLoadNextChapter = enabled)
        }
    }

    fun setChapterTransitionStyle(style: String) {
        viewModelScope.launch {
            repository.saveChapterTransitionStyle(style)
            _uiState.value = _uiState.value.copy(chapterTransitionStyle = style)
        }
    }

    fun setHoldScrollSpeed(speed: HoldScrollSpeed) {
        viewModelScope.launch {
            repository.saveHoldScrollSpeed(speed)
            _uiState.value = _uiState.value.copy(holdScrollSpeed = speed)
        }
    }

    fun retryPage(page: ReaderItem.Page) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manga = _uiState.value.activeManga ?: return@launch
                val pages = repository.loadChapterPages(
                    chapterUri = page.chapter.fileUri,
                    mangaName = manga.name,
                    chapterFileName = page.chapter.originalFileName,
                    forceReextract = true
                )
                withContext(Dispatchers.Main) {
                    val updatedItems = _uiState.value.readerItems.map { item ->
                        if (item is ReaderItem.Page && item.chapter.originalFileName == page.chapter.originalFileName) {
                            val file = pages.getOrNull(item.pageIndex) ?: item.file
                            item.copy(file = file)
                        } else {
                            item
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        readerItems = updatedItems,
                        activePages = pages
                    )
                }
            } catch (e: Exception) {
                Log.e("ReaderVM", "Retry page failed: ${e.message}", e)
            }
        }
    }

    fun skipPage(page: ReaderItem.Page) {
        val nextIndex = page.pageIndex + 1
        if (nextIndex < page.totalPages) {
            updateReadingPage(nextIndex)
        }
    }

    fun retrySinglePage() {
        val manga = _uiState.value.activeManga ?: return
        val chapter = _uiState.value.activeChapter ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = repository.loadChapterPages(
                    chapterUri = chapter.fileUri,
                    mangaName = manga.name,
                    chapterFileName = chapter.originalFileName,
                    forceReextract = true
                )
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(activePages = pages)
                }
            } catch (e: Exception) {
                Log.e("ReaderVM", "Retry single page failed: ${e.message}", e)
            }
        }
    }

    fun skipSinglePage() {
        val current = _uiState.value.currentPageIndex
        val total = _uiState.value.totalPages
        if (current < total - 1) {
            updateReadingPage(current + 1)
        }
    }

    fun setPagedRightTapAction(action: TapAction) {
        viewModelScope.launch {
            repository.savePagedRightTapAction(action)
            _uiState.value = _uiState.value.copy(pagedRightTapAction = action)
        }
    }

    fun setPagedLeftTapAction(action: TapAction) {
        viewModelScope.launch {
            repository.savePagedLeftTapAction(action)
            _uiState.value = _uiState.value.copy(pagedLeftTapAction = action)
        }
    }

    fun setReversePageNavigation(reverse: Boolean) {
        viewModelScope.launch {
            repository.saveReversePageNavigation(reverse)
            _uiState.value = _uiState.value.copy(reversePageNavigation = reverse)
        }
    }

    fun setHoldScrollSpeedPx(speedPx: Int) {
        viewModelScope.launch {
            repository.saveHoldScrollSpeedPx(speedPx)
            _uiState.value = _uiState.value.copy(holdScrollSpeedPx = speedPx)
        }
    }

    fun setHoldActivationDelayMs(delayMs: Int) {
        viewModelScope.launch {
            repository.saveHoldActivationDelayMs(delayMs)
            _uiState.value = _uiState.value.copy(holdActivationDelayMs = delayMs)
        }
    }

    fun setReverseHoldScrollDirection(reverse: Boolean) {
        viewModelScope.launch {
            repository.saveReverseHoldScrollDirection(reverse)
            _uiState.value = _uiState.value.copy(reverseHoldScrollDirection = reverse)
        }
    }

    fun setVerticalTapScrollPercent(percent: Int) {
        viewModelScope.launch {
            repository.saveVerticalTapScrollPercent(percent)
            _uiState.value = _uiState.value.copy(verticalTapScrollPercent = percent)
        }
    }

    fun showSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        debouncedSaveJob?.cancel()
        prefetchJob?.cancel()
        flushPendingReadingProgress()
    }
}
