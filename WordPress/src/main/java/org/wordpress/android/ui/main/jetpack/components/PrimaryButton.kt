package org.wordpress.android.ui.main.jetpack.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
            onClick = onClick,
            elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
            ),
            modifier = modifier
                    .padding(top = 20.dp, bottom = 10.dp)
                    .padding(horizontal = 30.dp)
                    .fillMaxWidth(),
    ) {
        Text(text = text)
    }
}
