package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.util.ImageFormatDetector
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import java.io.File

@Composable
fun ZoomableImage(
    file: File,
    contentDescription: String?,
    onTap: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        if (newScale > 1f) {
            val maxOffset = 600f * (newScale - 1f)
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxOffset, maxOffset),
                y = (offset.y + offsetChange.y).coerceIn(-maxOffset, maxOffset)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .then(
                if (scale > 1f) {
                    Modifier.transformable(state = transformState)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .crossfade(false)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryText.copy(alpha = 0.5f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            error = { state ->
                val reason = ImageFormatDetector.diagnoseFailure(file, state.result.throwable)
                Log.e("ZoomableImage", "Failed to render ${file.absolutePath}: Reason: $reason", state.result.throwable)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = HighlightRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Page could not be loaded",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: $reason",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            if (onRetry != null) {
                                OutlinedButton(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText)
                                ) {
                                    Text("Retry", fontSize = 12.sp)
                                }
                            }
                            if (onSkip != null) {
                                OutlinedButton(
                                    onClick = onSkip,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText)
                                ) {
                                    Text("Skip", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
