package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Compact numeric wheel picker for rapid page navigation.
 * Inspired by scrolling number wheels used in clock and timer applications.
 *
 * - Vertically scrollable with 5 visible rows.
 * - Scrolling upwards increases the selected page number.
 * - Scrolling downwards decreases the selected page number.
 * - Tapping any number immediately selects that page and dismisses the dialog.
 * - Clamped strictly between 1 and totalPages.
 * - Zero full image decoding overhead.
 */
@Composable
fun PageWheelPickerDialog(
    initialPage: Int, // 1-based current page
    totalPages: Int,
    chapterTitle: String = "",
    onPageSelected: (Int) -> Unit, // 1-based selected page
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val safeInitialPage = initialPage.coerceIn(1, safeTotalPages)
    val itemHeight = 44.dp

    // Initialize list centered around safeInitialPage - 1
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (safeInitialPage - 1).coerceIn(0, safeTotalPages - 1)
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Center detection: identify the page closest to the vertical center of the viewport
    val centeredPage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }
            (closestItem?.index?.plus(1) ?: (listState.firstVisibleItemIndex + 1)).coerceIn(1, safeTotalPages)
        }
    }

    // Scroll to initial page on opening
    LaunchedEffect(safeInitialPage) {
        listState.scrollToItem((safeInitialPage - 1).coerceIn(0, safeTotalPages - 1))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondaryBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Jump to Page",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
                if (chapterTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryText,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header displaying formatted target vs total: "042 / 186"
                val formattedCentered = String.format(Locale.US, "%03d", centeredPage)
                val formattedTotal = if (safeTotalPages >= 100) {
                    String.format(Locale.US, "%03d", safeTotalPages)
                } else {
                    safeTotalPages.toString()
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$formattedCentered / $formattedTotal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighlightRed,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.testTag("wheel_header_display")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Wheel Container (height = 5 items = 220.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(itemHeight * 5)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color(0xFF282830), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center selection indicator lens
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .padding(horizontal = 8.dp)
                            .background(Color(0xFF2A2A34), RoundedCornerShape(8.dp))
                            .border(1.dp, HighlightRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    )

                    // Scrollable Wheel
                    LazyColumn(
                        state = listState,
                        flingBehavior = snapFlingBehavior,
                        contentPadding = PaddingValues(vertical = itemHeight * 2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("numeric_wheel_lazy_column")
                    ) {
                        items(safeTotalPages) { index ->
                            val page = index + 1
                            val isSelected = page == centeredPage
                            val distanceFromCenter = kotlin.math.abs(page - centeredPage)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeight)
                                    .clickable {
                                        onPageSelected(page)
                                        onDismiss()
                                    }
                                    .testTag("wheel_page_item_$page"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%03d", page),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = when (distanceFromCenter) {
                                            0 -> 22.sp
                                            1 -> 17.sp
                                            else -> 13.sp
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when (distanceFromCenter) {
                                            0 -> HighlightRed
                                            1 -> PrimaryText.copy(alpha = 0.6f)
                                            else -> SecondaryText.copy(alpha = 0.35f)
                                        }
                                    )
                                )
                            }
                        }
                    }

                    // Top & Bottom shadow gradients for cylindrical depth effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 1.5f)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF141418),
                                        Color(0xFF141418).copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 1.5f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF141418).copy(alpha = 0.7f),
                                        Color(0xFF141418)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick step navigation buttons (-10, -1, +1, +10)
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val target = (centeredPage - 10).coerceAtLeast(1)
                            coroutineScope.launch { listState.animateScrollToItem(target - 1) }
                        },
                        modifier = Modifier.testTag("wheel_step_minus_10"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("-10", fontSize = 11.sp, color = SecondaryText)
                    }

                    OutlinedButton(
                        onClick = {
                            val target = (centeredPage - 1).coerceAtLeast(1)
                            coroutineScope.launch { listState.animateScrollToItem(target - 1) }
                        },
                        modifier = Modifier.testTag("wheel_step_minus_1"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("-1", fontSize = 11.sp, color = SecondaryText)
                    }

                    OutlinedButton(
                        onClick = {
                            val target = (centeredPage + 1).coerceAtMost(safeTotalPages)
                            coroutineScope.launch { listState.animateScrollToItem(target - 1) }
                        },
                        modifier = Modifier.testTag("wheel_step_plus_1"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+1", fontSize = 11.sp, color = SecondaryText)
                    }

                    OutlinedButton(
                        onClick = {
                            val target = (centeredPage + 10).coerceAtMost(safeTotalPages)
                            coroutineScope.launch { listState.animateScrollToItem(target - 1) }
                        },
                        modifier = Modifier.testTag("wheel_step_plus_10"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+10", fontSize = 11.sp, color = SecondaryText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPageSelected(centeredPage)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighlightRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("wheel_confirm_button")
            ) {
                Text(
                    text = "Go to Page $centeredPage",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("wheel_cancel_button")
            ) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
