package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.Chapter
import com.example.model.HoldScrollSpeed
import com.example.model.Manga
import com.example.model.ReaderItem
import com.example.model.ReadingDirection
import com.example.model.ReadingMode
import com.example.model.TapAction
import com.example.ui.components.PageWheelPickerDialog
import com.example.ui.components.ReaderSettingsSheet
import com.example.ui.components.ZoomableImage
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.util.ImageFormatDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun ReaderScreen(
    manga: Manga,
    chapter: Chapter,
    pages: List<File>,
    readerItems: List<ReaderItem> = emptyList(),
    nextChapter: Chapter? = null,
    chapterTransitionsEnabled: Boolean = true,
    chapterTransitionStyle: String = "SHOW_TITLE",
    isLoading: Boolean,
    errorMessage: String?,
    initialPageIndex: Int,
    readingMode: ReadingMode,
    readingDirection: ReadingDirection,
    holdScrollSpeed: HoldScrollSpeed = HoldScrollSpeed.NORMAL,
    pagedRightTapAction: TapAction = TapAction.NEXT_PAGE,
    pagedLeftTapAction: TapAction = TapAction.PREVIOUS_PAGE,
    reversePageNavigation: Boolean = false,
    holdScrollSpeedPx: Int = 650,
    holdActivationDelayMs: Int = 300,
    reverseHoldScrollDirection: Boolean = false,
    verticalTapScrollPercent: Int = 50,
    isBookmarked: Boolean,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onPageChange: (Int) -> Unit,
    onContinuousItemVisible: (ReaderItem, Int) -> Unit = { _, _ -> },
    onToggleBookmark: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onToggleReadingMode: (ReadingMode) -> Unit,
    onToggleReadingDirection: (ReadingDirection) -> Unit,
    onSetHoldScrollSpeed: (HoldScrollSpeed) -> Unit = {},
    onSetHoldScrollSpeedPx: (Int) -> Unit = {},
    onSetHoldActivationDelayMs: (Int) -> Unit = {},
    onSetReverseHoldScrollDirection: (Boolean) -> Unit = {},
    onSetVerticalTapScrollPercent: (Int) -> Unit = {},
    onSetPagedRightTapAction: (TapAction) -> Unit = {},
    onSetPagedLeftTapAction: (TapAction) -> Unit = {},
    onSetReversePageNavigation: (Boolean) -> Unit = {},
    onRetryPage: (ReaderItem.Page) -> Unit = {},
    onSkipPage: (ReaderItem.Page) -> Unit = {},
    onRetrySinglePage: () -> Unit = {},
    onSkipSinglePage: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var controlsVisible by remember { mutableStateOf(false) }
    var showWheelPicker by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var jumpToPageTrigger by remember { mutableStateOf<Int?>(null) }
    var activeCurrentPage by remember(chapter.originalFileName, initialPageIndex) {
        androidx.compose.runtime.mutableIntStateOf(initialPageIndex)
    }
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .testTag("reader_screen")
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val leftThreshold = totalWidth * 0.30f
        val rightThreshold = totalWidth * 0.70f

        when {
            isLoading && pages.isEmpty() && readerItems.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryText,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Preparing ${chapter.displayName}...",
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracting and sorting images",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            errorMessage != null && pages.isEmpty() && readerItems.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = HighlightRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to read chapter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighlightRed
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onBack) {
                        Text("Return to chapter list", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                }
            }

            pages.isEmpty() && readerItems.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = HighlightRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No images found in chapter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This archive appears to be empty or contains no supported image formats (.jpg, .png, .webp, .gif).",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onBack) {
                        Text("Return to chapter list", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                }
            }

            else -> {
                // Reading content
                if (readingMode == ReadingMode.SINGLE_PAGE) {
                    // --- Single Page Mode ---
                    val safeInitialPage = initialPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                    val pagerState = rememberPagerState(
                        initialPage = safeInitialPage,
                        pageCount = { pages.size }
                    )

                    // Track page changes
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            activeCurrentPage = page
                            onPageChange(page)
                        }
                    }

                    // Jump to page trigger (from wheel picker)
                    LaunchedEffect(jumpToPageTrigger) {
                        jumpToPageTrigger?.let { targetPage ->
                            if (pages.isNotEmpty()) {
                                val safe = targetPage.coerceIn(0, pages.size - 1)
                                pagerState.scrollToPage(safe)
                                activeCurrentPage = safe
                                onPageChange(safe)
                            }
                            jumpToPageTrigger = null
                        }
                    }

                    val layoutDir = if (readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(
                                totalWidth,
                                pagedRightTapAction,
                                pagedLeftTapAction,
                                reversePageNavigation,
                                pages.size,
                                hasPreviousChapter,
                                hasNextChapter
                            ) {
                                val touchSlop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downX = down.position.x
                                    val downY = down.position.y
                                    var hasMovedPastSlop = false
                                    val pointerId = down.id

                                    val zone = when {
                                        downX < leftThreshold -> -1 // Left 30%
                                        downX > rightThreshold -> 1  // Right 30%
                                        else -> 0                    // Center 40%
                                    }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            if (!hasMovedPastSlop) {
                                                if (zone == 0) {
                                                    // Center tap: toggle controls overlay
                                                    controlsVisible = !controlsVisible
                                                } else {
                                                    // Determine tap action
                                                    val rawAction = if (zone == 1) pagedRightTapAction else pagedLeftTapAction
                                                    val effectiveAction = if (reversePageNavigation) {
                                                        if (rawAction == TapAction.NEXT_PAGE) TapAction.PREVIOUS_PAGE else TapAction.NEXT_PAGE
                                                    } else {
                                                        rawAction
                                                    }

                                                    when (effectiveAction) {
                                                        TapAction.NEXT_PAGE -> {
                                                            coroutineScope.launch {
                                                                if (pagerState.currentPage < pages.size - 1) {
                                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                                } else if (hasNextChapter) {
                                                                    onNextChapter()
                                                                }
                                                            }
                                                        }
                                                        TapAction.PREVIOUS_PAGE -> {
                                                            coroutineScope.launch {
                                                                if (pagerState.currentPage > 0) {
                                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                                } else if (hasPreviousChapter) {
                                                                    onPreviousChapter()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            break
                                        }

                                        val dx = change.position.x - downX
                                        val dy = change.position.y - downY
                                        if (dx * dx + dy * dy > touchSlop * touchSlop) {
                                            hasMovedPastSlop = true
                                        }
                                    }
                                }
                            }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("single_page_pager"),
                                pageSpacing = 0.dp
                            ) { page ->
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    ZoomableImage(
                                        file = pages[page],
                                        contentDescription = "Page ${page + 1} of ${pages.size}",
                                        onTap = { controlsVisible = !controlsVisible },
                                        onRetry = onRetrySinglePage,
                                        onSkip = onSkipSinglePage,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // End of chapter prompt in single-page mode
                    if (hasNextChapter && nextChapter != null && pagerState.currentPage == pages.size - 1) {
                        Surface(
                            color = Color(0xF21C1C22),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                                .testTag("single_page_next_chapter_prompt")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chapter Complete",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Next: ${nextChapter.displayName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = onNextChapter,
                                    colors = ButtonDefaults.textButtonColors(contentColor = HighlightRed),
                                    modifier = Modifier.testTag("single_page_next_button")
                                ) {
                                    Text("Next Chapter", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // --- Vertical Continuous Mode (Webtoon / Manhwa) ---
                    val effectiveItems = if (readerItems.isNotEmpty()) {
                        readerItems
                    } else {
                        pages.mapIndexed { idx, file ->
                            ReaderItem.Page(
                                chapter = chapter,
                                pageIndex = idx,
                                totalPages = pages.size,
                                file = file
                            )
                        }
                    }

                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (effectiveItems.size - 1).coerceAtLeast(0))
                    )

                    var isHolding by remember { mutableStateOf(false) }
                    var holdDirection by remember { mutableFloatStateOf(0f) }

                    // Reset gestures when chapter or reading mode changes
                    LaunchedEffect(chapter.originalFileName, readingMode) {
                        isHolding = false
                        holdDirection = 0f
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            isHolding = false
                            holdDirection = 0f
                        }
                    }

                    // Continuous scrolling ticker at exact pixels-per-second speed
                    LaunchedEffect(isHolding, holdDirection, holdScrollSpeedPx, reverseHoldScrollDirection) {
                        if (isHolding && holdDirection != 0f) {
                            val dirMultiplier = if (reverseHoldScrollDirection) -holdDirection else holdDirection
                            var lastTimeNanos = System.nanoTime()
                            while (isActive && isHolding) {
                                val nowNanos = System.nanoTime()
                                val dtSec = (nowNanos - lastTimeNanos) / 1_000_000_000f
                                lastTimeNanos = nowNanos
                                val deltaPx = dirMultiplier * holdScrollSpeedPx * dtSec
                                if (deltaPx != 0f) {
                                    listState.scrollBy(deltaPx)
                                }
                                delay(16) // ~60fps ticker
                            }
                        }
                    }

                    // Jump to page trigger (from wheel picker)
                    LaunchedEffect(jumpToPageTrigger) {
                        jumpToPageTrigger?.let { targetPage ->
                            if (effectiveItems.isNotEmpty()) {
                                val safe = targetPage.coerceIn(0, effectiveItems.size - 1)
                                listState.scrollToItem(safe)
                                activeCurrentPage = safe
                                onPageChange(safe)
                            }
                            jumpToPageTrigger = null
                        }
                    }

                    LaunchedEffect(listState, effectiveItems) {
                        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                            activeCurrentPage = index
                            val item = effectiveItems.getOrNull(index)
                            if (item != null) {
                                onContinuousItemVisible(item, index)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(totalWidth, totalHeight, holdActivationDelayMs) {
                                val touchSlop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downX = down.position.x
                                    val downY = down.position.y
                                    var hasMovedPastSlop = false
                                    var holdTriggered = false
                                    val pointerId = down.id

                                    val zone = when {
                                        downX < leftThreshold -> -1 // Left 30%
                                        downX > rightThreshold -> 1  // Right 30%
                                        else -> 0                    // Center 40%
                                    }

                                    val holdJob = if (zone != 0) {
                                        coroutineScope.launch {
                                            delay(holdActivationDelayMs.toLong())
                                            if (!hasMovedPastSlop) {
                                                holdTriggered = true
                                                holdDirection = if (zone < 0) -1f else 1f
                                                isHolding = true
                                            }
                                        }
                                    } else null

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            holdJob?.cancel()
                                            if (isHolding) {
                                                isHolding = false
                                                holdDirection = 0f
                                            } else if (!hasMovedPastSlop && !holdTriggered) {
                                                // Clean tap within zone
                                                val tapFraction = (verticalTapScrollPercent / 100f).coerceIn(0.2f, 1.0f)
                                                when (zone) {
                                                    0 -> {
                                                        controlsVisible = !controlsVisible
                                                    }
                                                    -1 -> {
                                                        // Left tap: scroll up by configured tap-scroll percentage
                                                        coroutineScope.launch {
                                                            listState.animateScrollBy(-totalHeight * tapFraction)
                                                        }
                                                    }
                                                    1 -> {
                                                        // Right tap: scroll down by configured tap-scroll percentage
                                                        coroutineScope.launch {
                                                            listState.animateScrollBy(totalHeight * tapFraction)
                                                        }
                                                    }
                                                }
                                            }
                                            break
                                        }

                                        // Check if user dragged finger to scroll manually
                                        val dx = change.position.x - downX
                                        val dy = change.position.y - downY
                                        if (dx * dx + dy * dy > touchSlop * touchSlop) {
                                            hasMovedPastSlop = true
                                            holdJob?.cancel()
                                            if (isHolding) {
                                                isHolding = false
                                                holdDirection = 0f
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("vertical_continuous_list"),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(
                                count = effectiveItems.size,
                                key = { index ->
                                    when (val itm = effectiveItems[index]) {
                                        is ReaderItem.Page -> itm.uniqueKey
                                        is ReaderItem.ChapterSeparator -> itm.uniqueKey
                                        is ReaderItem.SeriesEnd -> itm.uniqueKey
                                    }
                                }
                            ) { index ->
                                when (val item = effectiveItems[index]) {
                                    is ReaderItem.Page -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 240.dp)
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(item.file)
                                                    .crossfade(false)
                                                    .build(),
                                                contentDescription = "Page ${item.pageIndex + 1} of ${item.totalPages}",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier.fillMaxWidth(),
                                                loading = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(300.dp)
                                                            .background(Color(0xFF161616)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            color = PrimaryText.copy(alpha = 0.4f),
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                },
                                                error = { state ->
                                                    val reason = ImageFormatDetector.diagnoseFailure(item.file, state.result.throwable)
                                                    Log.e("VerticalReader", "Error rendering ${item.file.name}: Reason: $reason")
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .defaultMinSize(minHeight = 160.dp)
                                                            .background(Color(0xFF1C1C1C))
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.BrokenImage,
                                                                contentDescription = null,
                                                                tint = HighlightRed,
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = "Page ${item.pageIndex + 1} could not be loaded",
                                                                color = PrimaryText,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Reason: $reason",
                                                                color = SecondaryText,
                                                                fontSize = 11.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                OutlinedButton(
                                                                    onClick = { onRetryPage(item) },
                                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText),
                                                                    modifier = Modifier.height(32.dp)
                                                                ) {
                                                                    Text("Retry", fontSize = 11.sp)
                                                                }
                                                                OutlinedButton(
                                                                    onClick = { onSkipPage(item) },
                                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText),
                                                                    modifier = Modifier.height(32.dp)
                                                                ) {
                                                                    Text("Skip", fontSize = 11.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    is ReaderItem.ChapterSeparator -> {
                                        ChapterSeparatorView(
                                            previousChapter = item.previousChapter,
                                            nextChapter = item.nextChapter,
                                            style = chapterTransitionStyle
                                        )
                                    }

                                    is ReaderItem.SeriesEnd -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F0F12))
                                                .padding(vertical = 48.dp, horizontal = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "You have reached the end of the collection.",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = SecondaryText,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TOP CONTROLS OVERLAY (Respects Status Bar WindowInsets) ---
        AnimatedVisibility(
            visible = controlsVisible && (pages.isNotEmpty() || readerItems.isNotEmpty()),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xF218181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to chapters",
                            tint = PrimaryText
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = chapter.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = manga.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SecondaryText,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                                tint = if (isBookmarked) HighlightRed else PrimaryText
                            )
                        }

                        IconButton(
                            onClick = { showReaderSettingsSheet = true },
                            modifier = Modifier.testTag("reader_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Reading Settings",
                                tint = PrimaryText
                            )
                        }
                    }
                }
            }
        }

        // --- BOTTOM CONTROLS OVERLAY (Respects Navigation Bar WindowInsets) ---
        AnimatedVisibility(
            visible = controlsVisible && (pages.isNotEmpty() || readerItems.isNotEmpty()),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color(0xF218181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- COMPACT PAGE INDICATOR: "042 / 186" (Replaces non-functional slider) ---
                    val totalDisplay = if (chapter.totalPages > 0) chapter.totalPages else pages.size.coerceAtLeast(1)
                    val safeCurrentPage = (activeCurrentPage + 1).coerceIn(1, totalDisplay)
                    val currentFormatted = String.format(Locale.US, "%03d", safeCurrentPage)
                    val totalFormatted = if (totalDisplay >= 100) {
                        String.format(Locale.US, "%03d", totalDisplay)
                    } else {
                        totalDisplay.toString()
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222228))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showWheelPicker = true },
                                    onLongPress = { showWheelPicker = true }
                                )
                            }
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .testTag("reader_page_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HighlightRed,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.testTag("reader_current_page_number")
                            )
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = SecondaryText.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = totalFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryText.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.testTag("reader_total_page_number")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mode toggles, direction / speed & Chapter navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Chapter
                        IconButton(
                            onClick = onPreviousChapter,
                            enabled = hasPreviousChapter,
                            modifier = Modifier.testTag("reader_prev_chapter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous chapter",
                                tint = if (hasPreviousChapter) PrimaryText else SecondaryText.copy(alpha = 0.3f)
                            )
                        }

                        // Reading Mode Selector
                        TextButton(
                            onClick = {
                                val newMode = if (readingMode == ReadingMode.SINGLE_PAGE) {
                                    ReadingMode.VERTICAL_CONTINUOUS
                                } else {
                                    ReadingMode.SINGLE_PAGE
                                }
                                onToggleReadingMode(newMode)
                            },
                            modifier = Modifier.testTag("reader_reading_mode_button")
                        ) {
                            Icon(
                                imageVector = if (readingMode == ReadingMode.SINGLE_PAGE) {
                                    Icons.Default.ViewCarousel
                                } else {
                                    Icons.Default.ViewAgenda
                                },
                                contentDescription = null,
                                tint = PrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (readingMode == ReadingMode.SINGLE_PAGE) "Single" else "Vertical",
                                color = PrimaryText,
                                fontSize = 12.sp
                            )
                        }

                        // Reading Direction Selector (in single-page mode) or Speed Indicator (in vertical mode)
                        if (readingMode == ReadingMode.SINGLE_PAGE) {
                            TextButton(
                                onClick = {
                                    val newDir = if (readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
                                        ReadingDirection.LEFT_TO_RIGHT
                                    } else {
                                        ReadingDirection.RIGHT_TO_LEFT
                                    }
                                    onToggleReadingDirection(newDir)
                                },
                                modifier = Modifier.testTag("reader_direction_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = PrimaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (readingDirection == ReadingDirection.RIGHT_TO_LEFT) "RTL" else "LTR",
                                    color = PrimaryText,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { showReaderSettingsSheet = true },
                                modifier = Modifier.testTag("reader_speed_button")
                            ) {
                                Text(
                                    text = "$holdScrollSpeedPx px/s",
                                    color = HighlightRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // In-reader Settings Quick Button
                        IconButton(
                            onClick = { showReaderSettingsSheet = true },
                            modifier = Modifier.testTag("reader_bottom_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PrimaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Next Chapter
                        IconButton(
                            onClick = onNextChapter,
                            enabled = hasNextChapter,
                            modifier = Modifier.testTag("reader_next_chapter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next chapter",
                                tint = if (hasNextChapter) PrimaryText else SecondaryText.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // --- DIALOGS & BOTTOM SHEETS ---
        if (showWheelPicker) {
            val totalDisplay = if (chapter.totalPages > 0) chapter.totalPages else pages.size.coerceAtLeast(1)
            PageWheelPickerDialog(
                initialPage = (activeCurrentPage + 1).coerceIn(1, totalDisplay),
                totalPages = totalDisplay,
                chapterTitle = chapter.displayName,
                onPageSelected = { selectedPage1Based ->
                    jumpToPageTrigger = selectedPage1Based - 1
                },
                onDismiss = { showWheelPicker = false }
            )
        }

        if (showReaderSettingsSheet) {
            ReaderSettingsSheet(
                readingMode = readingMode,
                readingDirection = readingDirection,
                verticalTapScrollPercent = verticalTapScrollPercent,
                holdScrollSpeedPx = holdScrollSpeedPx,
                holdActivationDelayMs = holdActivationDelayMs,
                reverseHoldScrollDirection = reverseHoldScrollDirection,
                pagedRightTapAction = pagedRightTapAction,
                pagedLeftTapAction = pagedLeftTapAction,
                reversePageNavigation = reversePageNavigation,
                onSetReadingMode = onToggleReadingMode,
                onSetReadingDirection = onToggleReadingDirection,
                onSetVerticalTapScrollPercent = onSetVerticalTapScrollPercent,
                onSetHoldScrollSpeedPx = onSetHoldScrollSpeedPx,
                onSetHoldActivationDelayMs = onSetHoldActivationDelayMs,
                onSetReverseHoldScrollDirection = onSetReverseHoldScrollDirection,
                onSetPagedRightTapAction = onSetPagedRightTapAction,
                onSetPagedLeftTapAction = onSetPagedLeftTapAction,
                onSetReversePageNavigation = onSetReversePageNavigation,
                onDismiss = { showReaderSettingsSheet = false }
            )
        }
    }
}

/**
 * Visually distinct, minimal chapter separator between consecutive chapters in vertical continuous reading.
 */
@Composable
fun ChapterSeparatorView(
    previousChapter: Chapter,
    nextChapter: Chapter,
    style: String = "SHOW_TITLE",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .padding(vertical = 36.dp, horizontal = 24.dp)
            .testTag("chapter_separator_${previousChapter.displayName}_${nextChapter.displayName}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.width(60.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (style == "SHOW_TITLE") {
            Text(
                text = "END OF ${previousChapter.displayName.uppercase()}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SecondaryText.copy(alpha = 0.85f),
                    letterSpacing = 1.4.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = nextChapter.displayName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryText,
                    letterSpacing = 1.1.sp
                ),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "NEXT CHAPTER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SecondaryText.copy(alpha = 0.85f),
                    letterSpacing = 1.4.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Continue reading",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HighlightRed,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = HighlightRed,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(
            modifier = Modifier.width(60.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
    }
}
