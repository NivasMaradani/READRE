package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Chapter
import com.example.model.Manga
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    manga: Manga,
    chapters: List<Chapter>,
    isLoading: Boolean,
    isBackgroundReconciling: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onChapterClick: (Chapter) -> Unit,
    onToggleBookmark: (Chapter) -> Unit,
    onMarkCompleted: (Chapter) -> Unit = {},
    onMarkUnread: (Chapter) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredChapters by remember(chapters, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                chapters
            } else {
                chapters.filter {
                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.originalFileName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            onSearchQueryChange("")
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MainBackground,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search chapters...", color = SecondaryText, fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                cursorColor = PrimaryText,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chapter_search_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = SecondaryText
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = manga.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (chapters.size == 1) "1 chapter" else "${chapters.size} chapters",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                )
                                if (isBackgroundReconciling) {
                                    Text(
                                        text = " • Syncing...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = HighlightRed,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                onSearchQueryChange("")
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("chapter_list_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to library",
                            tint = PrimaryText
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("chapter_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search chapters",
                                tint = PrimaryText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Non-blocking incremental sync indicator
            AnimatedVisibility(
                visible = isBackgroundReconciling,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = HighlightRed,
                    trackColor = SecondaryBackground
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && chapters.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryText)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Indexing chapters...", color = SecondaryText, fontSize = 14.sp)
                        }
                    }

                    chapters.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No chapters found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add .zip or .cbz archives to ${manga.name} folder to read chapters.",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }

                    filteredChapters.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No matching chapters",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No chapter matches '$searchQuery'",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chapter_list"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = filteredChapters,
                                key = { it.originalFileName }
                            ) { chapter ->
                                ChapterListItem(
                                    chapter = chapter,
                                    onClick = { onChapterClick(chapter) },
                                    onToggleBookmark = { onToggleBookmark(chapter) },
                                    onMarkCompleted = { onMarkCompleted(chapter) },
                                    onMarkUnread = { onMarkUnread(chapter) }
                                )
                                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnread: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Subtle brightness distinction: completed chapters have reduced opacity
    val rowAlpha = if (chapter.isCompleted) 0.50f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chapter_item_${chapter.displayName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and Metadata (clean, minimalist, restrained monochrome)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chapter.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (chapter.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = PrimaryText.copy(alpha = rowAlpha)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // In-progress chapter: simple neutral text indicator (e.g. Page 3 of 25)
                if (!chapter.isCompleted && chapter.currentPageIndex > 0) {
                    val progressText = if (chapter.totalPages > 0) {
                        "Page ${chapter.currentPageIndex + 1} of ${chapter.totalPages}"
                    } else {
                        "Page ${chapter.currentPageIndex + 1}"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = chapter.originalFileName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText.copy(alpha = if (chapter.isCompleted) 0.45f else 0.75f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (chapter.fileSize > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatFileSize(chapter.fileSize),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryText.copy(alpha = if (chapter.isCompleted) 0.35f else 0.6f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // Bookmark action icon (independent, monochrome)
        IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier
                .size(40.dp)
                .testTag("bookmark_button_${chapter.displayName}")
        ) {
            Icon(
                imageVector = if (chapter.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (chapter.isBookmarked) "Remove bookmark" else "Add bookmark",
                tint = if (chapter.isBookmarked) PrimaryText.copy(alpha = 0.85f) else SecondaryText.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Context / overflow menu for Mark as Completed / Unread
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chapter_menu_${chapter.displayName}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Chapter actions",
                    tint = SecondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(CardBackground)
            ) {
                if (!chapter.isCompleted) {
                    DropdownMenuItem(
                        text = { Text("Mark as Completed", color = PrimaryText) },
                        leadingIcon = {
                            Icon(Icons.Default.Done, contentDescription = null, tint = Color(0xFF4CAF50))
                        },
                        onClick = {
                            menuExpanded = false
                            onMarkCompleted()
                        },
                        modifier = Modifier.testTag("menu_mark_completed_${chapter.displayName}")
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Mark as Unread", color = PrimaryText) },
                        leadingIcon = {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = SecondaryText)
                        },
                        onClick = {
                            menuExpanded = false
                            onMarkUnread()
                        },
                        modifier = Modifier.testTag("menu_mark_unread_${chapter.displayName}")
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (chapter.isBookmarked) "Remove Bookmark" else "Bookmark Chapter", color = PrimaryText) },
                    leadingIcon = {
                        Icon(
                            if (chapter.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (chapter.isBookmarked) HighlightRed else SecondaryText
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleBookmark()
                    }
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.0f KB", kb)
    }
}
