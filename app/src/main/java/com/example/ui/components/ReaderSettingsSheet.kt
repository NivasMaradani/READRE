package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReadingDirection
import com.example.model.ReadingMode
import com.example.model.TapAction
import com.example.ui.theme.DividerColor
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import kotlin.math.roundToInt

/**
 * Compact in-reader reading settings sheet.
 *
 * Allows real-time tuning of reading parameters directly inside the reader
 * without having to navigate away to application settings.
 * All changes write through to the single source of truth in ReaderRepository,
 * ensuring bidirectional synchronization with general settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    readingMode: ReadingMode,
    readingDirection: ReadingDirection,
    verticalTapScrollPercent: Int,
    holdScrollSpeedPx: Int,
    holdActivationDelayMs: Int,
    reverseHoldScrollDirection: Boolean,
    pagedRightTapAction: TapAction,
    pagedLeftTapAction: TapAction,
    reversePageNavigation: Boolean,
    onSetReadingMode: (ReadingMode) -> Unit,
    onSetReadingDirection: (ReadingDirection) -> Unit,
    onSetVerticalTapScrollPercent: (Int) -> Unit,
    onSetHoldScrollSpeedPx: (Int) -> Unit,
    onSetHoldActivationDelayMs: (Int) -> Unit,
    onSetReverseHoldScrollDirection: (Boolean) -> Unit,
    onSetPagedRightTapAction: (TapAction) -> Unit,
    onSetPagedLeftTapAction: (TapAction) -> Unit,
    onSetReversePageNavigation: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SecondaryBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF4A4A56)) },
        modifier = Modifier.testTag("reader_settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = HighlightRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reading Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            fontSize = 17.sp
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_reader_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = DividerColor)

            // --- 1. READING MODE TOGGLE ---
            Column {
                Text(
                    text = "Reading Mode",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSingle = readingMode == ReadingMode.SINGLE_PAGE
                    OutlinedButton(
                        onClick = { onSetReadingMode(ReadingMode.SINGLE_PAGE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reader_sheet_mode_single"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSingle) Color(0xFF2E2E36) else Color.Transparent,
                            contentColor = if (isSingle) PrimaryText else SecondaryText
                        )
                    ) {
                        Text(
                            text = "Single Page",
                            fontSize = 12.sp,
                            fontWeight = if (isSingle) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    val isVertical = readingMode == ReadingMode.VERTICAL_CONTINUOUS
                    OutlinedButton(
                        onClick = { onSetReadingMode(ReadingMode.VERTICAL_CONTINUOUS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reader_sheet_mode_vertical"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isVertical) Color(0xFF2E2E36) else Color.Transparent,
                            contentColor = if (isVertical) PrimaryText else SecondaryText
                        )
                    ) {
                        Text(
                            text = "Vertical Webtoon",
                            fontSize = 12.sp,
                            fontWeight = if (isVertical) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            // --- 2. MODE-SPECIFIC CONTROLS ---
            if (readingMode == ReadingMode.VERTICAL_CONTINUOUS) {
                // === VERTICAL CONTINUOUS SETTINGS ===
                Text(
                    text = "Vertical Scrolling Controls",
                    color = HighlightRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Vertical Tap-Scroll Distance / Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap-Scroll Distance / Speed",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$verticalTapScrollPercent% of screen",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_tap_scroll_display")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = verticalTapScrollPercent.toFloat(),
                        onValueChange = { onSetVerticalTapScrollPercent(((it / 5f).roundToInt() * 5).coerceIn(20, 100)) },
                        valueRange = 20f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_tap_scroll_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )
                }

                // Hold-to-Scroll Speed (100 - 2000 px/s)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hold-to-Scroll Speed",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$holdScrollSpeedPx px/s",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_hold_speed_display")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = holdScrollSpeedPx.toFloat(),
                        onValueChange = { onSetHoldScrollSpeedPx(((it / 25f).roundToInt() * 25).coerceIn(100, 2000)) },
                        valueRange = 100f..2000f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_hold_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )
                }

                // Hold Activation Delay (150 - 800 ms)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hold Activation Delay",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$holdActivationDelayMs ms",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_hold_delay_display")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = holdActivationDelayMs.toFloat(),
                        onValueChange = { onSetHoldActivationDelayMs(((it / 25f).roundToInt() * 25).coerceIn(150, 800)) },
                        valueRange = 150f..800f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_hold_delay_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )
                }

                // Reverse Hold-Scroll Direction Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Reverse hold-scroll direction", color = PrimaryText, fontSize = 13.sp)
                        Text("Inverts continuous glide direction", color = SecondaryText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = reverseHoldScrollDirection,
                        onCheckedChange = onSetReverseHoldScrollDirection,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighlightRed
                        ),
                        modifier = Modifier.testTag("reader_switch_reverse_hold")
                    )
                }
            } else {
                // === SINGLE PAGE SETTINGS ===
                Text(
                    text = "Paged Navigation Controls",
                    color = HighlightRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Reading Direction (RTL vs LTR)
                Column {
                    Text(
                        text = "Page Order / Reading Direction",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRtl = readingDirection == ReadingDirection.RIGHT_TO_LEFT
                        OutlinedButton(
                            onClick = { onSetReadingDirection(ReadingDirection.RIGHT_TO_LEFT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_dir_rtl"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isRtl) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (isRtl) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("RTL (Manga)", fontSize = 12.sp, fontWeight = if (isRtl) FontWeight.Bold else FontWeight.Normal)
                        }

                        val isLtr = readingDirection == ReadingDirection.LEFT_TO_RIGHT
                        OutlinedButton(
                            onClick = { onSetReadingDirection(ReadingDirection.LEFT_TO_RIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_dir_ltr"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isLtr) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (isLtr) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("LTR (Comic)", fontSize = 12.sp, fontWeight = if (isLtr) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Right Tap Action
                Column {
                    Text(
                        text = "Right Tap Action",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rightIsNext = pagedRightTapAction == TapAction.NEXT_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedRightTapAction(TapAction.NEXT_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_right_tap_next"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rightIsNext) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (rightIsNext) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Next (Default)", fontSize = 12.sp, fontWeight = if (rightIsNext) FontWeight.Bold else FontWeight.Normal)
                        }

                        val rightIsPrev = pagedRightTapAction == TapAction.PREVIOUS_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedRightTapAction(TapAction.PREVIOUS_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_right_tap_prev"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rightIsPrev) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (rightIsPrev) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Previous", fontSize = 12.sp, fontWeight = if (rightIsPrev) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Left Tap Action
                Column {
                    Text(
                        text = "Left Tap Action",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val leftIsPrev = pagedLeftTapAction == TapAction.PREVIOUS_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedLeftTapAction(TapAction.PREVIOUS_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_left_tap_prev"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (leftIsPrev) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (leftIsPrev) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Previous (Default)", fontSize = 12.sp, fontWeight = if (leftIsPrev) FontWeight.Bold else FontWeight.Normal)
                        }

                        val leftIsNext = pagedLeftTapAction == TapAction.NEXT_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedLeftTapAction(TapAction.NEXT_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_sheet_left_tap_next"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (leftIsNext) Color(0xFF2E2E36) else Color.Transparent,
                                contentColor = if (leftIsNext) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Next", fontSize = 12.sp, fontWeight = if (leftIsNext) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Reverse Page Navigation Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Reverse page navigation", color = PrimaryText, fontSize = 13.sp)
                        Text("Inverts left and right tap actions", color = SecondaryText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = reversePageNavigation,
                        onCheckedChange = onSetReversePageNavigation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighlightRed
                        ),
                        modifier = Modifier.testTag("reader_switch_reverse_paged_nav")
                    )
                }
            }
        }
    }
}
