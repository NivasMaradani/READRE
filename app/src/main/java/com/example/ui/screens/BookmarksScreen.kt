package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BookmarkEntity
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    onRemoveBookmark: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group bookmarks by manga name
    val groupedBookmarks = bookmarks.groupBy { it.mangaName }

    Scaffold(
        modifier = modifier,
        containerColor = MainBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bookmarks",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
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
            if (bookmarks.isEmpty()) {
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
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No bookmarked chapters",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap the bookmark icon on any chapter or reading screen to save it here for fast access.",
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
                        .testTag("bookmarks_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedBookmarks.forEach { (mangaName, mangaBookmarks) ->
                        item(key = "header_$mangaName") {
                            Text(
                                text = mangaName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(mangaBookmarks, key = { "${it.mangaName}_${it.chapterFileName}" }) { bookmark ->
                            val cleanChapterName = bookmark.chapterFileName.substringBeforeLast('.')
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onBookmarkClick(bookmark) }
                                    .testTag("bookmark_item_${bookmark.chapterFileName}"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cleanChapterName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryText
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = bookmark.chapterFileName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SecondaryText,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onRemoveBookmark(bookmark) },
                                        modifier = Modifier.testTag("remove_bookmark_${bookmark.chapterFileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkRemove,
                                            contentDescription = "Remove bookmark",
                                            tint = HighlightRed,
                                            modifier = Modifier.size(20.dp)
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
}
