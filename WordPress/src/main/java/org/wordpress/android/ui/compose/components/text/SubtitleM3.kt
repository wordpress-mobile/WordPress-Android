package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun SubtitleM3(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 17.sp,
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 20.dp)
    )
}

@Preview
@Composable
private fun SubtitleM3Preview() {
    AppThemeM3 {
        SubtitleM3(text = "This subtitle should be long enough so the preview wraps to more than one line")
    }
}
