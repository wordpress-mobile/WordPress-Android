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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
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
import org.wordpress.android.ui.compose.theme.AppTheme

@OptIn(ExperimentalAnimationApi::class)
@Suppress("DEPRECATION")
@Composable
fun MicToStopIcon(state: VoiceToContentUiState.ReadyToRecord) {
    val isEnabled = state.isEligibleForFeature
    var isTapped by remember { mutableStateOf(false) }
    var isMic by remember { mutableStateOf(true) }
    val isLight = !isSystemInDarkTheme()

    // For tries 1-3
//    val innerCircleColor by animateColorAsState(
//        targetValue = if (isMic) MaterialTheme.colors.primary else if (isLight) Color.Black else Color.White
//    )

    // For try 4
    val circleColor by animateColorAsState(
        targetValue = if (isMic) MaterialTheme.colors.primary else if (isLight) Color.Black else Color.White, label = ""
    )

    val iconColor by animateColorAsState(
        targetValue =
            if (isMic) Color.White else if (isLight) Color.White else Color.Black, label = ""
    )

    val micIcon: Painter = painterResource(id = R.drawable.v2c_mic)
    val stopIcon: Painter = painterResource(id = R.drawable.v2c_stop)

   // Tries 1-5
    // val icon: Painter = if (isMic) painterResource(id = R.drawable.v2c_mic) else painterResource(id = R.drawable.v2c_stop)

    // First try with the box showing that is not transparent
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .background(MaterialTheme.colors.primary, shape = CircleShape)
//            .clickable { isMic = !isMic }
//            .animateContentSize() // Ensures smooth resizing if needed
//    ) {
//        Box(
//            contentAlignment = Alignment.Center,
//            modifier = Modifier
//                .size(80.dp) // Adjust the size to make space for the inner circle
//                .background(innerCircleColor, shape = CircleShape)
//        ) {
//            Image(
//                painter = icon,
//                contentDescription = null,
//                modifier = Modifier.size(50.dp), // Adjust the size as needed
//                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor)
//            )
//        }
//    }

    // Second attempt with the box being transparent
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .clickable {
//                isMic = !isMic
//              // todo  myViewModel.onMicOrStopIconClicked(isMic)
//            }
//            .animateContentSize() // Ensures smooth resizing if needed
//    ) {
//        Box(
//            contentAlignment = Alignment.Center,
//            modifier = Modifier
//                .size(80.dp) // Adjust the size to make space for the inner circle
//                .background(MaterialTheme.colors.primary, shape = CircleShape)
//        ) {
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier
//                    .size(70.dp) // Adjust the size to make space for the inner circle
//                    .background(innerCircleColor, shape = CircleShape)
//            ) {
//                Image(
//                    painter = icon,
//                    contentDescription = null,
//                    modifier = Modifier.size(50.dp), // Adjust the size as needed
//                    colorFilter = ColorFilter.tint(iconColor)
//                )
//            }
//        }
//    }

    // Third attempt with the box being transparent and the inner circle being animated
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .clickable {
//                isMic = !isMic
//               // myViewModel.onMicOrStopIconClicked(isMic)
//            }
//    ) {
//        // Outer Circle
//        Box(
//            contentAlignment = Alignment.Center,
//            modifier = Modifier
//                .size(100.dp)
//                .background(MaterialTheme.colors.primary, shape = CircleShape)
//        ) {
//            // Inner Circle
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier
//                    .size(80.dp)
//                    .background(innerCircleColor, shape = CircleShape)
//            ) {
//                Image(
//                    painter = icon,
//                    contentDescription = null,
//                    modifier = Modifier.size(50.dp), // Adjust the size as needed
//                    colorFilter = ColorFilter.tint(iconColor)
//                )
//            }
//        }
//    }

    // Four attempt
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .background(circleColor, shape = CircleShape)
//            .clickable {
//                isMic = !isMic
//               //  myViewModel.onMicOrStopIconClicked(isMic)
//            }
//    ) {
//        Image(
//            painter = icon,
//            contentDescription = null,
//            modifier = Modifier.size(50.dp), // Adjust the size as needed
//            colorFilter = ColorFilter.tint(iconColor)
//        )
//    }

    // Attempt five
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .clickable {
//                isMic = !isMic
//               // myViewModel.onMicOrStopIconClicked(isMic)
//            }
//    ) {
//        Box(
//            modifier = Modifier
//                .size(100.dp)
//                .background(circleColor, shape = CircleShape)
//                .align(Alignment.Center)
//        ) {
//            Image(
//                painter = icon,
//                contentDescription = null,
//                modifier = Modifier
//                    .size(50.dp) // Adjust the size as needed
//                    .align(Alignment.Center),
//                colorFilter = ColorFilter.tint(iconColor)
//            )
//        }
//    }

//    // Attempt six
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier
//            .size(100.dp) // Adjust the size as needed
//            .clickable {
//                isMic = !isMic
//               // myViewModel.onMicOrStopIconClicked(isMic)
//            }
//    ) {
//        Box(
//            modifier = Modifier
//                .size(100.dp)
//                .background(circleColor, shape = CircleShape)
//        )
//
//        AnimatedContent(
//            targetState = isMic,
//            transitionSpec = {
//                fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
//            }, label = ""
//        ) { targetState ->
//            val icon: Painter = if (targetState) painterResource(id = R.drawable.v2c_mic) else painterResource(id = R.drawable.v2c_stop)
//            Image(
//                painter = icon,
//                contentDescription = null,
//                modifier = Modifier.size(50.dp), // Adjust the size as needed
//                colorFilter = ColorFilter.tint(iconColor)
//            )
//        }
//    }
    // Attempt 7
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .clickable(
                enabled = isEnabled && !isTapped,
                onClick = {
                    if (isMic) {
                        isMic = false
                        isTapped = true
                        // todo: state.onMicTap()
                    } else {
                       // todo: nothing yet state.onMicTap
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(circleColor, shape = CircleShape)
        )

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
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExistingLayoutPreview() {
    AppTheme {
        MicToStopIcon(VoiceToContentUiState.ReadyToRecord(
            header = R.string.voice_to_content_ready_to_record,
            labelText = R.string.voice_to_content_ready_to_record_label,
            subLabelText = R.string.voice_to_content_tap_to_start,
            requestsAvailable = 0,
            isEligibleForFeature = true,
            onMicTap = {},
            onCloseAction = {},
            hasPermission = true,
            onRequestPermission = {}
        ))
    }
}
