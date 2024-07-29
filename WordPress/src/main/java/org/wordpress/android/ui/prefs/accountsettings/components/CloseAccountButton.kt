package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.domains.management.M3Theme

@Composable
fun CloseAccountButton(onClick: () -> Unit = {}): Unit = Button(
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.error,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.error,

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
    M3Theme {
        CloseAccountButton()
    }
}

