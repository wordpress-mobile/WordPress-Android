package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String
) {
    Button(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background)
    ) {
        Text(text = text)
    }
}
