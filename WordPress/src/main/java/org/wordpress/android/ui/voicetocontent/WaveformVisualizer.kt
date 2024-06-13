package org.wordpress.android.ui.voicetocontent

// import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
// import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
// port androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary
) {
    val spacingDp = 8.dp
    val density = LocalDensity.current
    val spacingPx = with(density) { spacingDp.toPx() } // 2dp spacing between bars
    val strokeWidth = with(density) { 2.dp.toPx() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Calculate the number of lines that can fit within the width given the spacing
        val numberOfLines = (width / spacingPx).toInt()

        // Adjust amplitudeStep to match the number of lines we can fit
        val amplitudeStep = maxOf(1, amplitudes.size / numberOfLines)

        for (i in 0 until numberOfLines) {
            val index = i * amplitudeStep
            if (index >= amplitudes.size) break

            val x = i * spacingPx
            val amplitude = amplitudes[index]
            val y1 = centerY - (amplitude * height * 0.5f)
            val y2 = centerY + (amplitude * height * 0.5f)

            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, y1),
                end = androidx.compose.ui.geometry.Offset(x, y2),
                strokeWidth = strokeWidth
            )
        }
    }
}
//
//private fun Float.toPx(density: Density): Float {
//    return with(density) { this@toPx.dp.toPx() }
//}


    // Try three
//    Canvas(modifier = modifier) {
//        val width = size.width
//        val height = size.height
//        val centerY = height / 2
//        val stepWidth = width / (amplitudes.size.toFloat() - 1)
//
//        for (i in amplitudes.indices) {
//            val x = i * stepWidth
//            val amplitude = amplitudes[i]
//            val y1 = centerY - (amplitude * height * 0.5f)
//            val y2 = centerY + (amplitude * height * 0.5f)
//
//            drawLine(
//                color = color,
//                start = androidx.compose.ui.geometry.Offset(x, y1),
//                end = androidx.compose.ui.geometry.Offset(x, y2),
//                strokeWidth = 2.dp.toPx()
//            )
//        }
//    }
    // Try two
//    val infiniteTransition = rememberInfiniteTransition(label = "")
//    val phase by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 2000, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart
//        ), label = ""
//    )
//
//    Canvas(modifier = modifier) {
//        val width = size.width
//        val height = size.height
//        val centerY = height / 2
//
//        //  val amplitudeStep = maxOf(1, amplitudes.size / width.toInt())
//        val stepWidth = width / amplitudes.size.toFloat()
//
//        for (i in amplitudes.indices) {
//            // for (i in amplitudes.indices step amplitudeStep) {
//            // Calculate the x-coordinate based on phase to create a scrolling effect
//            val x = (i * stepWidth + phase * width) % width
//            val amplitude = amplitudes[i]
//            val y1 = centerY - (amplitude * height * 0.5f)
//            val y2 = centerY + (amplitude * height * 0.5f)
//
//            drawLine(
//                color = color,
//                start = androidx.compose.ui.geometry.Offset(x, y1),
//                end = androidx.compose.ui.geometry.Offset(x, y2),
//                strokeWidth = 2.dp.toPx()
//            )
//        }


    // Try One
//    val infiniteTransition = rememberInfiniteTransition(label = "")
//    val phase by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 1000, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart
//        ), label = ""
//    )
//
//    Canvas(modifier = modifier) {
//        val width = size.width
//        val height = size.height
//        val centerY = height / 2
//        val amplitudeStep = maxOf(1, amplitudes.size / width.toInt())
//
//        for (i in amplitudes.indices step amplitudeStep) {
//            val x = ((i / amplitudeStep) + phase * width) % width
//            val amplitude = amplitudes[i]
//            val y1 = centerY - (amplitude * height * 0.5f)
//            val y2 = centerY + (amplitude * height * 0.5f)
//
//            drawLine(
//                color = color,
//                start = androidx.compose.ui.geometry.Offset(x, y1),
//                end = androidx.compose.ui.geometry.Offset(x, y2),
//                strokeWidth = 2.dp.toPx()
//            )
//        }
//    }
//}

