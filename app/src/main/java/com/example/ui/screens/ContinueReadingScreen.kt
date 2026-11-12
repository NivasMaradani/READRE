package com.example.ui.screens

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterReadingProgressEntity
import com.example.data.entity.ReadingProgressEntity
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueReadingScreen(
    continueReadingList: List<ReadingProgressEntity>,
    historyList: List<ChapterReadingProgressEntity>,
    onResumeManga: (ReadingProgressEntity) -> Unit,
    onOpenHistoryItem: (ChapterReadingProgressEntity) -> Unit,
    onDeleteHistoryItem: (ChapterReadingProgressEntity) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = CardBackground,
            title = { Text("Clear Reading History", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear your entire reading history? Saved manga bookmarks will remain unaffected.", color = SecondaryText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllHistory()
                    }
                ) {
                    Text("Clear All", color = HighlightRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MainBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History & Continue",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = SecondaryText
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (continueReadingList.isEmpty() && historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SecondaryBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No reading history",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Chapters you read will appear here with your saved reading position.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SecondaryText,
                            lineHeight = 20.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_list"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // 1. Continue Reading Shelf (Manga-Level resume pointers)
                    if (continueReadingList.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = HighlightRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Continue Reading",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(continueReadingList, key = { it.mangaName }) { progress ->
                                        ContinueReadingCard(
                                            progress = progress,
                                            onClick = { onResumeManga(progress) }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = DividerColor.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    // 2. Reading History (Chronological list of all read chapters)
                    if (historyList.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reading History",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                )
                                Text(
                                    text = "${historyList.size} entries",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        items(
                            items = historyList,
                            key = { "${it.mangaName}_${it.chapterFileName}" }
                        ) { item ->
                            HistoryListItem(
                                item = item,
                                onClick = { onOpenHistoryItem(item) },
                                onDelete = { onDeleteHistoryItem(item) }
                            )
                            HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    progress: ReadingProgressEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick)
            .testTag("continue_card_${progress.mangaName}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SecondaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = HighlightRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.mangaName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Page ${progress.pageIndex + 1}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HighlightRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = progress.chapterFileName.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SecondaryText,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatRelativeTime(progress.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Resume",
                    tint = PrimaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryListItem(
    item: ChapterReadingProgressEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("history_item_${item.chapterFileName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator leading
        Box(
            modifier = Modifier
                .size(36.dp)
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Surface(
                    color = HighlightRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (item.totalPages > 0) "p.${item.currentPageIndex + 1}/${item.totalPages}" else "p.${item.currentPageIndex + 1}",
                        color = HighlightRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.mangaName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.chapterFileName.substringBeforeLast('.'),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${formatRelativeTime(item.lastReadTimestamp)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(36.dp)
                .testTag("delete_history_${item.chapterFileName}")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from history",
                tint = SecondaryText.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        diff < 172800_000L -> "Yesterday"
        else -> DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.DAY_IN_MILLIS).toString()
    }
}
