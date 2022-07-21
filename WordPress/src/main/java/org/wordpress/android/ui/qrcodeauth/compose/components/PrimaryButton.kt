package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String
) {
    Button(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled
    ) {
        Text(text = text)
    }
}
