package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.FontSize

@Composable
fun Title(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = FontSize.ExtraExtraExtraLarge.value,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 30.dp)
    )
}

@Preview
@Composable
private fun TitlePreview() {
    AppThemeM2 {
        Title(text = "This title should be long enough so the preview wraps to more than one line")
    }
}
