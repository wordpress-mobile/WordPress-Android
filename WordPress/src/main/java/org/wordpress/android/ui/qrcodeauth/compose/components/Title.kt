package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun Title(text: String) {
    Text(
            modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                            start = Margin.ExtraExtraMediumLarge.value,
                            end = Margin.ExtraExtraMediumLarge.value,
                            top = Margin.Medium.value,
                            bottom = Margin.Medium.value
                    ),
            text = text,
            textAlign = TextAlign.Center,
            fontSize = FontSize.DoubleExtraLarge.value
    )
}
