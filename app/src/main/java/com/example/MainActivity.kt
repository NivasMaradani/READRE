package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Manga
import com.example.ui.components.AppTab
import com.example.ui.components.ChangePinDialog
import com.example.ui.components.PinAuthDialog
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.ReaderBottomBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TutorialDialog
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ChapterListScreen
import com.example.ui.screens.ContinueReadingScreen
import com.example.ui.screens.HiddenLibraryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MainBackground
import com.example.ui.theme.ReaderTheme
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReaderTheme {
                ReaderApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ReaderApp(viewModel: ReaderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val readingProgress by viewModel.allReadingProgress.collectAsStateWithLifecycle()
    val chapterHistory by viewModel.allChapterHistory.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var mangaForCoverSelection by remember { mutableStateOf<Manga?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val manga = mangaForCoverSelection
        if (uri != null && manga != null) {
            viewModel.setCustomCover(manga, uri)
        }
        mangaForCoverSelection = null
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (uiState.showSettings) {
        SettingsDialog(
            currentFolderUri = uiState.readerFolderUri,
            defaultReadingMode = uiState.defaultReadingMode,
            defaultReadingDirection = uiState.defaultReadingDirection,
            pagedRightTapAction = uiState.pagedRightTapAction,
            pagedLeftTapAction = uiState.pagedLeftTapAction,
            reversePageNavigation = uiState.reversePageNavigation,
            holdScrollSpeedPx = uiState.holdScrollSpeedPx,
            holdActivationDelayMs = uiState.holdActivationDelayMs,
            reverseHoldScrollDirection = uiState.reverseHoldScrollDirection,
            verticalTapScrollPercent = uiState.verticalTapScrollPercent,
            chapterTransitionsEnabled = uiState.chapterTransitionsEnabled,
            autoLoadNextChapter = uiState.autoLoadNextChapter,
            chapterTransitionStyle = uiState.chapterTransitionStyle,
            onChangeFolder = {
                viewModel.showSettings(false)
                folderPickerLauncher.launch(null)
            },
            onRescanLibrary = {
                viewModel.rescanLibrary()
            },
            onSetDefaultReadingMode = { mode ->
                viewModel.setDefaultReadingMode(mode)
            },
            onSetDefaultReadingDirection = { dir ->
                viewModel.setDefaultReadingDirection(dir)
            },
            onSetPagedRightTapAction = { action ->
                viewModel.setPagedRightTapAction(action)
            },
            onSetPagedLeftTapAction = { action ->
                viewModel.setPagedLeftTapAction(action)
            },
            onSetReversePageNavigation = { reverse ->
                viewModel.setReversePageNavigation(reverse)
            },
            onSetHoldScrollSpeedPx = { speedPx ->
                viewModel.setHoldScrollSpeedPx(speedPx)
            },
            onSetHoldActivationDelayMs = { delayMs ->
                viewModel.setHoldActivationDelayMs(delayMs)
            },
            onSetReverseHoldScrollDirection = { reverse ->
                viewModel.setReverseHoldScrollDirection(reverse)
            },
            onSetVerticalTapScrollPercent = { pct ->
                viewModel.setVerticalTapScrollPercent(pct)
            },
            onSetChapterTransitionsEnabled = { enabled ->
                viewModel.setChapterTransitionsEnabled(enabled)
            },
            onSetAutoLoadNextChapter = { enabled ->
                viewModel.setAutoLoadNextChapter(enabled)
            },
            onSetChapterTransitionStyle = { style ->
                viewModel.setChapterTransitionStyle(style)
            },
            onActivateHiddenLibrary = {
                viewModel.onActivateHiddenLibrary()
            },
            onReplayTutorial = {
                viewModel.showTutorial(true)
            },
            onDismiss = { viewModel.showSettings(false) }
        )
    }

    if (uiState.showPinSetupDialog) {
        PinSetupDialog(
            onPinSet = { pin -> viewModel.setupPin(pin) },
            onDismiss = { viewModel.showPinSetupDialog(false) }
        )
    }

    if (uiState.showPinAuthDialog) {
        PinAuthDialog(
            onVerifyPin = { pin -> viewModel.verifyPin(pin) },
            onSuccess = { viewModel.onPinAuthSuccess() },
            onChangePinRequest = {
                viewModel.showPinAuthDialog(false)
                viewModel.showChangePinDialog(true)
            },
            onDismiss = { viewModel.showPinAuthDialog(false) }
        )
    }

    if (uiState.showChangePinDialog) {
        ChangePinDialog(
            onChangePin = { oldPin, newPin -> viewModel.changePin(oldPin, newPin) },
            onSuccess = { viewModel.showChangePinDialog(false) },
            onDismiss = { viewModel.showChangePinDialog(false) }
        )
    }

    if (uiState.showTutorialDialog) {
        TutorialDialog(
            onComplete = { viewModel.completeTutorial() },
            onSkip = { viewModel.skipTutorial() }
        )
    }

    when (uiState.screen) {
        ScreenState.WELCOME -> {
            WelcomeScreen(
                isLoading = uiState.isScanning,
                errorMessage = uiState.errorMessage,
                onFolderSelected = { uri -> viewModel.onFolderSelected(uri) }
            )
        }

        ScreenState.CHAPTER_LIST -> {
            val manga = uiState.selectedManga
            if (manga != null) {
                ChapterListScreen(
                    manga = manga,
                    chapters = uiState.chapters,
                    isLoading = uiState.isScanning,
                    isBackgroundReconciling = uiState.isBackgroundReconciling,
                    searchQuery = uiState.chapterSearchQuery,
                    onSearchQueryChange = { query -> viewModel.setChapterSearchQuery(query) },
                    onChapterClick = { chapter -> viewModel.openChapter(chapter) },
                    onToggleBookmark = { chapter -> viewModel.toggleChapterBookmarkFromList(chapter) },
                    onMarkCompleted = { chapter -> viewModel.markChapterCompleted(chapter) },
                    onMarkUnread = { chapter -> viewModel.markChapterUnread(chapter) },
                    onBack = { viewModel.closeChapterList() }
                )
            }
        }

        ScreenState.READER -> {
            val manga = uiState.activeManga
            val chapter = uiState.activeChapter
            if (manga != null && chapter != null) {
                ReaderScreen(
                    manga = manga,
                    chapter = chapter,
                    pages = uiState.activePages,
                    readerItems = uiState.readerItems,
                    nextChapter = uiState.nextChapter,
                    chapterTransitionsEnabled = uiState.chapterTransitionsEnabled,
                    chapterTransitionStyle = uiState.chapterTransitionStyle,
                    isLoading = uiState.isLoadingPages,
                    errorMessage = uiState.errorMessage,
                    initialPageIndex = uiState.currentPageIndex,
                    readingMode = uiState.readingMode,
                    readingDirection = uiState.readingDirection,
                    holdScrollSpeed = uiState.holdScrollSpeed,
                    pagedRightTapAction = uiState.pagedRightTapAction,
                    pagedLeftTapAction = uiState.pagedLeftTapAction,
                    reversePageNavigation = uiState.reversePageNavigation,
                    holdScrollSpeedPx = uiState.holdScrollSpeedPx,
                    holdActivationDelayMs = uiState.holdActivationDelayMs,
                    reverseHoldScrollDirection = uiState.reverseHoldScrollDirection,
                    verticalTapScrollPercent = uiState.verticalTapScrollPercent,
                    isBookmarked = uiState.isCurrentChapterBookmarked,
                    hasPreviousChapter = uiState.hasPreviousChapter,
                    hasNextChapter = uiState.hasNextChapter,
                    onPageChange = { pageIndex -> viewModel.updateReadingPage(pageIndex) },
                    onContinuousItemVisible = { item, index -> viewModel.onContinuousItemVisible(item, index) },
                    onToggleBookmark = { viewModel.toggleCurrentChapterBookmark() },
                    onPreviousChapter = { viewModel.previousChapter() },
                    onNextChapter = { viewModel.nextChapter() },
                    onToggleReadingMode = { mode -> viewModel.setReadingMode(mode) },
                    onToggleReadingDirection = { dir -> viewModel.setReadingDirection(dir) },
                    onSetHoldScrollSpeed = { speed -> viewModel.setHoldScrollSpeed(speed) },
                    onSetHoldScrollSpeedPx = { speedPx -> viewModel.setHoldScrollSpeedPx(speedPx) },
                    onSetHoldActivationDelayMs = { delay -> viewModel.setHoldActivationDelayMs(delay) },
                    onSetReverseHoldScrollDirection = { rev -> viewModel.setReverseHoldScrollDirection(rev) },
                    onSetVerticalTapScrollPercent = { pct -> viewModel.setVerticalTapScrollPercent(pct) },
                    onSetPagedRightTapAction = { act -> viewModel.setPagedRightTapAction(act) },
                    onSetPagedLeftTapAction = { act -> viewModel.setPagedLeftTapAction(act) },
                    onSetReversePageNavigation = { rev -> viewModel.setReversePageNavigation(rev) },
                    onRetryPage = { page -> viewModel.retryPage(page) },
                    onSkipPage = { page -> viewModel.skipPage(page) },
                    onRetrySinglePage = { viewModel.retrySinglePage() },
                    onSkipSinglePage = { viewModel.skipSinglePage() },
                    onBack = { viewModel.closeReader() }
                )
            }
        }

        ScreenState.MAIN_TABS -> {
            Scaffold(
                containerColor = MainBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    ReaderBottomBar(
                        currentTab = uiState.currentTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (uiState.currentTab) {
                        AppTab.LIBRARY -> {
                            LibraryScreen(
                                mangaList = uiState.mangaList,
                                isScanning = uiState.isScanning,
                                onMangaClick = { manga -> viewModel.openManga(manga) },
                                onChangeCover = { manga ->
                                    mangaForCoverSelection = manga
                                    coverPickerLauncher.launch("image/*")
                                },
                                onResetCover = { manga ->
                                    viewModel.resetCustomCover(manga)
                                },
                                onRegenerateCover = { manga ->
                                    viewModel.regenerateAutoCover(manga)
                                },
                                onHideManga = { manga ->
                                    viewModel.hideManga(manga)
                                },
                                onRefresh = { viewModel.rescanLibrary() },
                                onOpenSettings = { viewModel.showSettings(true) }
                            )
                        }

                        AppTab.BOOKMARKS -> {
                            BookmarksScreen(
                                bookmarks = bookmarks,
                                onBookmarkClick = { bookmark -> viewModel.openBookmark(bookmark) },
                                onRemoveBookmark = { bookmark ->
                                    viewModel.removeBookmark(bookmark.mangaName, bookmark.chapterFileName)
                                }
                            )
                        }

                        AppTab.CONTINUE_READING -> {
                            ContinueReadingScreen(
                                continueReadingList = readingProgress,
                                historyList = chapterHistory,
                                onResumeManga = { progress -> viewModel.resumeMangaProgress(progress) },
                                onOpenHistoryItem = { historyItem -> viewModel.openHistoryItem(historyItem) },
                                onDeleteHistoryItem = { historyItem -> viewModel.deleteHistoryItem(historyItem) },
                                onClearAllHistory = { viewModel.clearAllHistory() }
                            )
                        }
                    }
                }
            }
        }

        ScreenState.HIDDEN_LIBRARY -> {
            HiddenLibraryScreen(
                mangaList = uiState.hiddenMangaList,
                onMangaClick = { manga -> viewModel.openManga(manga) },
                onUnhideManga = { manga -> viewModel.unhideManga(manga) },
                onChangeCover = { manga ->
                    mangaForCoverSelection = manga
                    coverPickerLauncher.launch("image/*")
                },
                onResetCover = { manga ->
                    viewModel.resetCustomCover(manga)
                },
                onRegenerateCover = { manga ->
                    viewModel.regenerateAutoCover(manga)
                },
                onChangePinRequest = {
                    viewModel.showChangePinDialog(true)
                },
                onLock = {
                    viewModel.lockHiddenLibrary()
                }
            )
        }
    }
}
