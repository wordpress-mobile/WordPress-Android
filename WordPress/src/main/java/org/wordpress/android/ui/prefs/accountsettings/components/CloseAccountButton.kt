package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun CloseAccountButton(onClick: () -> Unit = {}) = Button(
    elevation = ButtonDefaults.elevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    colors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Transparent,
        contentColor = MaterialTheme.colors.error,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colors.error,
    ),
    modifier = Modifier
        .fillMaxWidth(),
    onClick = onClick,
) {
    Text(
        text = stringResource(R.string.close_account),
        modifier = Modifier
            .padding(10.dp),
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCloseAccountButton() {
    AppTheme {
        CloseAccountButton()
    }
}

