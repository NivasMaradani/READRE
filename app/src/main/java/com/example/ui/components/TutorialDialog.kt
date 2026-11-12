package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class TutorialStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val description: String,
    val detailBadge: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        title = "Welcome to Reader",
        subtitle = "100% Offline & Private",
        icon = Icons.Default.WifiOff,
        description = "Reader is completely offline. No accounts, no cloud sync, no tracking, and no ads. Your manga collection stays strictly on your device.",
        detailBadge = "Fully Offline"
    ),
    TutorialStep(
        title = "Library & Fast Indexing",
        subtitle = "ZIP & CBZ Support",
        icon = Icons.Default.Folder,
        description = "Choose your local manga directory via Android Storage Access Framework. Reader indexes 1,000–5,000+ chapters in milliseconds with cached metadata.",
        detailBadge = "Zero-Wait Loading"
    ),
    TutorialStep(
        title = "Covers & Hidden Manga",
        subtitle = "Customization & Privacy",
        icon = Icons.Default.Image,
        description = "Automatic covers are generated in the background from chapter 1. You can choose custom artwork, regenerate covers, or hide private titles behind a secure PIN.",
        detailBadge = "Cover Controls"
    ),
    TutorialStep(
        title = "Minimalist Chapter Lists",
        subtitle = "Distraction-Free Tracking",
        icon = Icons.Default.FormatListNumbered,
        description = "Clean chapter rows without loud checkmarks. Completed chapters appear gently dimmed, and in-progress chapters show subtle neutral page counts.",
        detailBadge = "Clean Rows"
    ),
    TutorialStep(
        title = "Reader Controls & Taps",
        subtitle = "Intuitive Page Navigation",
        icon = Icons.Default.TouchApp,
        description = "Tap the center of the reader to toggle the toolbar and slider. Tap the right side to turn the page forward, and the left side to go back.",
        detailBadge = "Center Controls"
    ),
    TutorialStep(
        title = "Reading Modes",
        subtitle = "Single Page & Webtoon",
        icon = Icons.Default.ViewCarousel,
        description = "Easily switch between Single Page mode (RTL manga & LTR comics) and seamless Vertical Continuous mode with uninterrupted chapter transitions.",
        detailBadge = "RTL, LTR & Webtoon"
    ),
    TutorialStep(
        title = "Hold-to-Scroll & Gestures",
        subtitle = "Custom Velocity & Reversal",
        icon = Icons.Default.Speed,
        description = "Press and hold in vertical mode to glide through long chapters hands-free. Customize your exact scroll speed (100–2000 px/s) and tap bindings in Settings.",
        detailBadge = "Continuous Scroll"
    ),
    TutorialStep(
        title = "Bookmarks & History",
        subtitle = "Never Lose Your Place",
        icon = Icons.Default.Bookmark,
        description = "Bookmark key moments with one tap. Access 'Continue Reading' to jump back into active series, or view your independent chapter reading history.",
        detailBadge = "Instant Resume"
    ),
    TutorialStep(
        title = "You're Ready to Read!",
        subtitle = "Replay Anytime in Settings",
        icon = Icons.Default.CheckCircle,
        description = "You can replay this tutorial anytime from Settings → Help & About → Replay Tutorial without affecting reading progress. Enjoy your reading!",
        detailBadge = "Ready!"
    )
)

@Composable
fun TutorialDialog(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[currentStepIndex]
    val totalSteps = TUTORIAL_STEPS.size

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = SecondaryBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tutorial (${currentStepIndex + 1}/$totalSteps)",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("tutorial_skip_button")
                ) {
                    Text("Skip", color = SecondaryText, fontSize = 12.sp)
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
                // Feature badge & visual card
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = HighlightRed,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HighlightRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = step.detailBadge.uppercase(),
                        color = HighlightRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PrimaryText.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Step progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        val isCurrent = i == currentStepIndex
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) HighlightRed else SecondaryText.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStepIndex < totalSteps - 1) {
                        currentStepIndex++
                    } else {
                        onComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("tutorial_next_button")
            ) {
                Text(
                    text = if (currentStepIndex == totalSteps - 1) "Get Started" else "Next",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                OutlinedButton(
                    onClick = { currentStepIndex-- },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("tutorial_back_button")
                ) {
                    Text("Back", color = PrimaryText)
                }
            }
        }
    )
}
