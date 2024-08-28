package org.wordpress.android.ui.voicetocontent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wordpress.android.util.audio.RecordingUpdate

@Composable
fun WaveformOblongVisualizer(recordingUpdate: RecordingUpdate, currentPosition: Int) {
    val amplitudeList = recordingUpdate.amplitudes
    val maxRadius = 150f // increased maximum radius for the oblongs
    val minRadius = 50f  // increased minimum radius for the oblongs
    val maxAmplitude = 32767f // maximum possible amplitude from MediaRecorder
    val oblongWidth = 20f // fixed width of the oblongs
    val color = MaterialTheme.colors.primary

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)) {
        val width = size.width
        val height = size.height
        val barSpacing = width / 20 // number of visible oblongs
        val visibleAmplitudes = amplitudeList.takeLast(currentPosition + 20).take(20)

        visibleAmplitudes.forEachIndexed { index, amplitude ->
            val normalizedAmplitude = amplitude.coerceIn(0f, maxAmplitude)
            val oblongHeight = minRadius + (normalizedAmplitude / maxAmplitude) * (maxRadius - minRadius)
            val xOffset = index * barSpacing
            val yOffset = (height - oblongHeight) / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(xOffset, yOffset),
                size = androidx.compose.ui.geometry.Size(oblongWidth, oblongHeight),
                cornerRadius = CornerRadius(10f, 10f) // rounded corners to make it oblong
            )
        }
    }
}

@Composable
fun ScrollingWaveformVisualizer(recordingUpdate: RecordingUpdate) {
    val currentPosition = remember { mutableIntStateOf(0) }
    LaunchedEffect(recordingUpdate) {
        while (true) {
            delay(100) // adjust delay as needed for scrolling speed
            currentPosition.intValue += 1
            if (currentPosition.intValue >= recordingUpdate.amplitudes.size) {
                currentPosition.intValue = 0 // reset to start if we reach the end
            }
        }
    }
    WaveformOblongVisualizer(recordingUpdate, currentPosition.intValue)
}

@Preview(showBackground = true)
@Composable
fun WaveformVisualizerOblongPreview() {
    val mockRecordingUpdate = RecordingUpdate(
        amplitudes = listOf(
            1000f, 5000f, 10000f, 20000f, 30000f, 15000f, 25000f, 12000f, 17000f, 11000f,
            1000f, 5000f, 10000f, 20000f, 30000f, 15000f, 25000f, 12000f, 17000f, 11000f
        )
    )
    WaveformOblongVisualizer(recordingUpdate = mockRecordingUpdate, currentPosition = 0)
}
