package org.wordpress.android.ui.prefs.accountsettings.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun FlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = AppColor.White,
    ),
    enabled: Boolean = true,
) = Button(
    modifier = modifier,
    onClick = onClick,
    colors = colors,
    elevation = ButtonDefaults.elevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    enabled = enabled,
) {
    Text(text)
}

@Composable
fun FlatOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    isPending: Boolean = false,
) = OutlinedButton(
    modifier = modifier,
    onClick = onClick,
    colors = colors,
    elevation = ButtonDefaults.elevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    enabled = enabled && !isPending,
) {
    if (isPending) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    } else {
        Text(text)
    }
}
