package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.theme.AppThemeM2

@Composable
fun Subtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 17.sp,
        style = TextStyle(letterSpacing = (-0.01).sp),
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 20.dp)
    )
}

@Preview
@Composable
private fun SubtitlePreview() {
    AppThemeM2 {
        Subtitle(text = "This subtitle should be long enough so the preview wraps to more than one line")
    }
}
