package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Manga
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenLibraryScreen(
    mangaList: List<Manga>,
    onMangaClick: (Manga) -> Unit,
    onUnhideManga: (Manga) -> Unit,
    onChangeCover: (Manga) -> Unit,
    onResetCover: (Manga) -> Unit,
    onRegenerateCover: (Manga) -> Unit,
    onChangePinRequest: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onLock()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MainBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = HighlightRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hidden Manga",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onLock,
                        modifier = Modifier.testTag("hidden_library_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Lock and Exit",
                            tint = PrimaryText
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onChangePinRequest,
                        modifier = Modifier.testTag("hidden_library_change_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = "Change PIN",
                            tint = PrimaryText
                        )
                    }
                    IconButton(
                        onClick = onLock,
                        modifier = Modifier.testTag("hidden_library_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Library",
                            tint = HighlightRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        },
        bottomBar = {
            // Privacy notice banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy Notice: This feature protects access through Reader. Original archives on disk are not encrypted.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (mangaList.isEmpty()) {
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No hidden manga",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To protect manga, open your normal library, tap the 3-dot menu on any manga card, and select 'Hide Manga'.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SecondaryText,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hidden_manga_grid")
                ) {
                    items(mangaList, key = { it.name }) { manga ->
                        HiddenMangaGridItem(
                            manga = manga,
                            onClick = { onMangaClick(manga) },
                            onUnhideManga = { onUnhideManga(manga) },
                            onChangeCover = { onChangeCover(manga) },
                            onResetCover = { onResetCover(manga) },
                            onRegenerateCover = { onRegenerateCover(manga) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenMangaGridItem(
    manga: Manga,
    onClick: () -> Unit,
    onUnhideManga: () -> Unit,
    onChangeCover: () -> Unit,
    onResetCover: () -> Unit,
    onRegenerateCover: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("hidden_manga_card_${manga.name}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .background(SecondaryBackground)
        ) {
            val coverFile = manga.coverFilePath?.let { File(it) }
            if (coverFile != null && coverFile.exists()) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = "Cover for ${manga.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Neutral placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = HighlightRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = manga.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Top right: Menu button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("hidden_cover_menu_${manga.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    DropdownMenuItem(
                        text = { Text("Unhide Manga", color = PrimaryText) },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = PrimaryText)
                        },
                        onClick = {
                            menuExpanded = false
                            onUnhideManga()
                        },
                        modifier = Modifier.testTag("menu_unhide_${manga.name}")
                    )
                    DropdownMenuItem(
                        text = { Text("Choose Custom Cover", color = PrimaryText) },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryText)
                        },
                        onClick = {
                            menuExpanded = false
                            onChangeCover()
                        },
                        modifier = Modifier.testTag("menu_change_cover_hidden_${manga.name}")
                    )
                    if (manga.hasCustomCover) {
                        DropdownMenuItem(
                            text = { Text("Restore Automatic Cover", color = HighlightRed) },
                            leadingIcon = {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = HighlightRed)
                            },
                            onClick = {
                                menuExpanded = false
                                onResetCover()
                            },
                            modifier = Modifier.testTag("menu_reset_cover_hidden_${manga.name}")
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Regenerate Automatic Cover", color = PrimaryText) },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryText)
                        },
                        onClick = {
                            menuExpanded = false
                            onRegenerateCover()
                        },
                        modifier = Modifier.testTag("menu_regenerate_cover_hidden_${manga.name}")
                    )
                }
            }

            // Bottom title overlay with chapter badge
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = manga.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (manga.chapterCount == 1) "1 ch" else "${manga.chapterCount} ch",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(HighlightRed.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HIDDEN",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
