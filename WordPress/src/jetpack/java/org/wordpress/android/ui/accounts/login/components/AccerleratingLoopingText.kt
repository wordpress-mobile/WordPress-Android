package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.isOdd


@Composable
fun LargeTexts() {
    val texts = stringArrayResource(R.array.login_prologue_revamped_jetpack_feature_texts)

    texts.forEachIndexed { index, text ->
        LargeText(
                text = text,
                color = when (index.isOdd) {
                    true -> colorResource(R.color.text_color_jetpack_login_feature_odd)
                    false -> colorResource(R.color.text_color_jetpack_login_feature_even)
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private val fontSize = 43.sp
private val lineHeight = fontSize * 0.95

@Composable
fun LargeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
            text = text,
            style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = lineHeight,
            ),
            color = color,
            modifier = modifier
    )
}

const val MAXIMUM_VELOCITY = 0.3f

/** This composable launches an effect to continuously update the position of the text based on a simplified physics
 *  model. Velocity and position are recalculated for each frame, with the resulting position update passed to the child
 *  composable RepeatingColumn, allowing the placement to be modified vertically without the need to recompose the
 *  nested children.
 *
 *  Velocity is constrained so that it does not fall below -MAXIMUM_VELOCITY and does not exceed MAXIMUM_VELOCITY.
 *  Position is constrained so that it always falls between 0 and 1, and represents the relative vertical offset in
 *  terms of the height of the repeated child composable.
 */
@Composable
fun AcceleratingLoopingText(acceleration: Float, modifier: Modifier = Modifier) {
    val currentAcceleration by rememberUpdatedState { acceleration }
    var velocity by remember { mutableStateOf(0f) }
    var position by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameNanos: Long? = null
        while(isActive) {
            val currentFrameNanos = awaitFrame()
            // Calculate elapsed time (in seconds) since the last frame
            val elapsed = (currentFrameNanos - (lastFrameNanos?: currentFrameNanos)) / 1e9.toFloat()
            // Update the velocity (clamped to the maximum)
            velocity =  (velocity + elapsed * currentAcceleration()).coerceIn(-MAXIMUM_VELOCITY, MAXIMUM_VELOCITY)
            // Update the position, modulo 1 (ensuring a value greater or equal to 0, and less than 1)
            position = ((position + elapsed * velocity) % 1 + 1) % 1
            // Update frame timestamp reference
            lastFrameNanos = currentFrameNanos
        }
    }
    RepeatingColumn(position, modifier = modifier) {
        LargeTexts()
    }
}


@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
fun PreviewLoopingText() {
    AppTheme {
        AcceleratingLoopingText(acceleration = 0.1f)
    }
}
