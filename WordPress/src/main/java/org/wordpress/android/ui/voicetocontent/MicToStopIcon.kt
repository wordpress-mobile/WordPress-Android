package org.wordpress.android.ui.voicetocontent

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2

@OptIn(ExperimentalAnimationApi::class)
@Suppress("DEPRECATION")
@Composable
fun MicToStopIcon(model: RecordingPanelUIModel, isRecording: Boolean) {
    val isEnabled = model.isEnabled
    val isMic by rememberUpdatedState(newValue = !isRecording)
    val isLight = !isSystemInDarkTheme()

    val circleColor by animateColorAsState(
        targetValue = if (!isEnabled) MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        else if (isMic) MaterialTheme.colors.primary
        else if (isLight) Color.Black
        else Color.White, label = ""
    )

    val iconColor by animateColorAsState(
        targetValue = if (!isEnabled) MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
        else if (isMic) Color.White
        else if (isLight) Color.White
        else Color.Black, label = ""
    )

    val micIcon: Painter = painterResource(id = R.drawable.ic_mic_none_24)
    val stopIcon: Painter = painterResource(id = R.drawable.v2c_stop)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .background(Color.Transparent) // Ensure transparent background
            .clickable(
                enabled = isEnabled,
                onClick = {
                    if (model.hasPermission) {
                        if (isMic) {
                            model.onMicTap?.invoke()
                        } else {
                            model.onStopTap?.invoke()
                        }
                    } else {
                        model.onRequestPermission?.invoke()
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(circleColor, shape = CircleShape)
        )
        if (model.hasPermission) {
            AnimatedContent(
                targetState = isMic,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                }, label = ""
            ) { targetState ->
                val icon: Painter = if (targetState) micIcon else stopIcon
                val iconSize = if (targetState) 50.dp else 35.dp
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        } else {
            // Display mic icon statically if permission is not granted
            Image(
                painter = micIcon,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExistingLayoutPreview() {
    AppThemeM2 {
        MicToStopIcon(
            RecordingPanelUIModel(
                isEligibleForFeature = true,
                onMicTap = {},
                onStopTap = {},
                hasPermission = true,
                onRequestPermission = {},
                actionLabel = R.string.voice_to_content_base_header_label, isEnabled = false
            ),
            isRecording = true
        )
    }
}
