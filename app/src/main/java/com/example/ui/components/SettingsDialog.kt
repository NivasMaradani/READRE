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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun SettingsDialog(
    currentFolderUri: String?,
    defaultReadingMode: ReadingMode,
    defaultReadingDirection: ReadingDirection,
    pagedRightTapAction: TapAction = TapAction.NEXT_PAGE,
    pagedLeftTapAction: TapAction = TapAction.PREVIOUS_PAGE,
    reversePageNavigation: Boolean = false,
    holdScrollSpeedPx: Int = 650,
    holdActivationDelayMs: Int = 300,
    reverseHoldScrollDirection: Boolean = false,
    verticalTapScrollPercent: Int = 50,
    chapterTransitionsEnabled: Boolean = true,
    autoLoadNextChapter: Boolean = true,
    chapterTransitionStyle: String = "SHOW_TITLE",
    onChangeFolder: () -> Unit,
    onRescanLibrary: () -> Unit,
    onSetDefaultReadingMode: (ReadingMode) -> Unit,
    onSetDefaultReadingDirection: (ReadingDirection) -> Unit,
    onSetPagedRightTapAction: (TapAction) -> Unit = {},
    onSetPagedLeftTapAction: (TapAction) -> Unit = {},
    onSetReversePageNavigation: (Boolean) -> Unit = {},
    onSetHoldScrollSpeedPx: (Int) -> Unit = {},
    onSetHoldActivationDelayMs: (Int) -> Unit = {},
    onSetReverseHoldScrollDirection: (Boolean) -> Unit = {},
    onSetVerticalTapScrollPercent: (Int) -> Unit = {},
    onSetChapterTransitionsEnabled: (Boolean) -> Unit = {},
    onSetAutoLoadNextChapter: (Boolean) -> Unit = {},
    onSetChapterTransitionStyle: (String) -> Unit = {},
    onActivateHiddenLibrary: () -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondaryBackground,
        title = {
            var tapCount by remember { mutableIntStateOf(0) }
            var lastTapTime by remember { mutableLongStateOf(0L) }

            Text(
                text = "Reader Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 3000L) {
                            tapCount = 1
                        } else {
                            tapCount++
                        }
                        lastTapTime = now
                        if (tapCount >= 7) {
                            tapCount = 0
                            onActivateHiddenLibrary()
                        }
                    }
                    .testTag("settings_title")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Folder management
                Column {
                    Text(
                        text = "Reader Folder",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentFolderUri ?: "No folder selected",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChangeFolder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_folder_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryText
                            )
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onRescanLibrary()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rescan_library_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryText
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Compact Reading Controls Section
                Column {
                    Text(
                        text = "Reading Controls",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Reading Mode Selector
                    Text(
                        text = "Default Reading Mode",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetDefaultReadingMode(ReadingMode.SINGLE_PAGE) }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = defaultReadingMode == ReadingMode.SINGLE_PAGE,
                            onClick = { onSetDefaultReadingMode(ReadingMode.SINGLE_PAGE) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryText,
                                unselectedColor = SecondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Single Page", color = PrimaryText, fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetDefaultReadingMode(ReadingMode.VERTICAL_CONTINUOUS) }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = defaultReadingMode == ReadingMode.VERTICAL_CONTINUOUS,
                            onClick = { onSetDefaultReadingMode(ReadingMode.VERTICAL_CONTINUOUS) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryText,
                                unselectedColor = SecondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vertical Continuous (Webtoon)", color = PrimaryText, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- SUBSECTION A: PAGED NAVIGATION ---
                    Text(
                        text = "Paged Navigation",
                        color = HighlightRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Reading Direction
                    Text(
                        text = "Page Order / Reading Direction",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRtl = defaultReadingDirection == ReadingDirection.RIGHT_TO_LEFT
                        OutlinedButton(
                            onClick = { onSetDefaultReadingDirection(ReadingDirection.RIGHT_TO_LEFT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dir_rtl"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isRtl) Color(0xFF2E2E33) else Color.Transparent,
                                contentColor = if (isRtl) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("RTL (Manga)", fontSize = 12.sp, fontWeight = if (isRtl) FontWeight.Bold else FontWeight.Normal)
                        }

                        val isLtr = defaultReadingDirection == ReadingDirection.LEFT_TO_RIGHT
                        OutlinedButton(
                            onClick = { onSetDefaultReadingDirection(ReadingDirection.LEFT_TO_RIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dir_ltr"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isLtr) Color(0xFF2E2E33) else Color.Transparent,
                                contentColor = if (isLtr) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("LTR (Comic)", fontSize = 12.sp, fontWeight = if (isLtr) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Right Tap Action
                    Text(
                        text = "Right Tap Action",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rightIsNext = pagedRightTapAction == TapAction.NEXT_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedRightTapAction(TapAction.NEXT_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("right_tap_next"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rightIsNext) Color(0xFF2E2E33) else Color.Transparent,
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
                                .testTag("right_tap_prev"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rightIsPrev) Color(0xFF2E2E33) else Color.Transparent,
                                contentColor = if (rightIsPrev) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Previous", fontSize = 12.sp, fontWeight = if (rightIsPrev) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Left Tap Action
                    Text(
                        text = "Left Tap Action",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val leftIsPrev = pagedLeftTapAction == TapAction.PREVIOUS_PAGE
                        OutlinedButton(
                            onClick = { onSetPagedLeftTapAction(TapAction.PREVIOUS_PAGE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("left_tap_prev"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (leftIsPrev) Color(0xFF2E2E33) else Color.Transparent,
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
                                .testTag("left_tap_next"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (leftIsNext) Color(0xFF2E2E33) else Color.Transparent,
                                contentColor = if (leftIsNext) PrimaryText else SecondaryText
                            )
                        ) {
                            Text("Next", fontSize = 12.sp, fontWeight = if (leftIsNext) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            modifier = Modifier.testTag("switch_reverse_paged_nav")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- SUBSECTION B: VERTICAL SCROLLING ---
                    Text(
                        text = "Vertical Scrolling",
                        color = HighlightRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tap-Scroll Distance / Speed Slider (20% - 100%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap-Scroll Distance / Speed",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$verticalTapScrollPercent% of screen",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("settings_tap_scroll_display")
                        )
                    }
                    Slider(
                        value = verticalTapScrollPercent.toFloat(),
                        onValueChange = { onSetVerticalTapScrollPercent(((it / 5f).roundToInt() * 5).coerceIn(20, 100)) },
                        valueRange = 20f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_tap_scroll_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continuous Hold-Scroll Speed Slider (100 - 2000 px/s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hold-to-Scroll Speed",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$holdScrollSpeedPx px/s",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("speed_display")
                        )
                    }
                    Slider(
                        value = holdScrollSpeedPx.toFloat(),
                        onValueChange = { onSetHoldScrollSpeedPx(((it / 25f).roundToInt() * 25).coerceIn(100, 2000)) },
                        valueRange = 100f..2000f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hold_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hold Activation Delay Slider (150 - 800 ms)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hold Activation Delay",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$holdActivationDelayMs ms",
                            color = HighlightRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("delay_display")
                        )
                    }
                    Slider(
                        value = holdActivationDelayMs.toFloat(),
                        onValueChange = { onSetHoldActivationDelayMs(((it / 25f).roundToInt() * 25).coerceIn(150, 800)) },
                        valueRange = 150f..800f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hold_delay_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightRed,
                            activeTrackColor = HighlightRed,
                            inactiveTrackColor = DividerColor
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

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
                            Text("Hold right scrolls up, hold left scrolls down", color = SecondaryText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = reverseHoldScrollDirection,
                            onCheckedChange = onSetReverseHoldScrollDirection,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HighlightRed
                            ),
                            modifier = Modifier.testTag("switch_reverse_hold_dir")
                        )
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Continuous Chapter Reading Settings
                Column {
                    Text(
                        text = "Continuous Chapter Transitions",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Auto load next chapter switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Automatically load next chapter", color = PrimaryText, fontSize = 13.sp)
                            Text("Seamlessly continue into next chapter in vertical mode", color = SecondaryText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoLoadNextChapter,
                            onCheckedChange = onSetAutoLoadNextChapter,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HighlightRed
                            ),
                            modifier = Modifier.testTag("switch_auto_load_next")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Chapter transitions switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Chapter transitions", color = PrimaryText, fontSize = 13.sp)
                            Text("Show separator between chapters", color = SecondaryText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = chapterTransitionsEnabled,
                            onCheckedChange = onSetChapterTransitionsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HighlightRed
                            ),
                            modifier = Modifier.testTag("switch_chapter_transitions")
                        )
                    }

                    if (chapterTransitionsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transition Appearance",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetChapterTransitionStyle("SHOW_TITLE") }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = chapterTransitionStyle == "SHOW_TITLE",
                                onClick = { onSetChapterTransitionStyle("SHOW_TITLE") },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryText,
                                    unselectedColor = SecondaryText
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Show chapter title (Default)", color = PrimaryText, fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetChapterTransitionStyle("MINIMAL") }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = chapterTransitionStyle == "MINIMAL",
                                onClick = { onSetChapterTransitionStyle("MINIMAL") },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryText,
                                    unselectedColor = SecondaryText
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Minimal", color = PrimaryText, fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Help & About Section
                Column {
                    Text(
                        text = "Help & About",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reader • Offline Manga & Manhwa Reader",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onReplayTutorial()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replay_tutorial_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Replay Tutorial", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("settings_done_button")
            ) {
                Text("Done", color = PrimaryText, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
