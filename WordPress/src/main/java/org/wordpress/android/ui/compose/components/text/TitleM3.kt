package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.FontSize

@Composable
fun TitleM3(
    text: String,
    modifier: Modifier = Modifier,
) {
    val fontSize = FontSize.ExtraExtraExtraLarge.value
    Text(
        text = text,
        fontSize = fontSize,
        lineHeight = fontSize * 1.25,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 30.dp)
    )
}

@Preview
@Composable
private fun TitleM3Preview() {
    AppThemeM3 {
        TitleM3(text = "This title should be long enough so the preview wraps to more than one line")
    }
}
