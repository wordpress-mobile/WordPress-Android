package org.wordpress.android.ui.main.jetpack.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.unit.FontSize

@Composable
fun Subtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
            text = text,
            fontSize = FontSize.ExtraLarge.value,
            modifier = modifier
                    .padding(horizontal = 30.dp)
                    .padding(top = 20.dp)
    )
}
