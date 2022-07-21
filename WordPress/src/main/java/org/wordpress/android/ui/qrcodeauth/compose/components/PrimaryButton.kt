package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String
) {
    Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                            vertical = Margin.Small.value,
                            horizontal = Margin.ExtraExtraMediumLarge.value,
                    )
    ) {
        Text(text = text)
    }
}
