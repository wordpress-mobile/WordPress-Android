package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun Title(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        fontSize = FontSize.DoubleExtraLarge.value,
        modifier = modifier
            .wrapContentSize()
            .padding(
                horizontal = Margin.ExtraExtraMediumLarge.value,
                vertical = Margin.Medium.value
            )
    )
}
