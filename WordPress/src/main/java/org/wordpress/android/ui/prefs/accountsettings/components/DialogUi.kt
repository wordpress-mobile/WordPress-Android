package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun DialogUi(
    currentUsername: String,
    isPending: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val padding = 10.dp
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = padding),
        textAlign = TextAlign.Center,
        text = stringResource(R.string.account_closure_dialog_title),
        fontWeight = FontWeight.Bold,
    )
    Text(stringResource(R.string.account_closure_dialog_message))
    TextField(
        modifier = Modifier
            .padding(vertical = padding)
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = username,
        onValueChange = { username = it },
        enabled = !isPending,
    )
    Row(
        modifier = Modifier
            .padding(vertical = padding)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        FlatOutlinedButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onSurface,
                backgroundColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colors.onSurface,
            ),
            enabled = !isPending,
        )
        Spacer(Modifier.size(padding))
        FlatOutlinedButton(
            text = stringResource(R.string.confirm),
            modifier = Modifier.weight(1f),
            enabled = username.isNotEmpty() && username == currentUsername,
            isPending = isPending,
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.error,
                backgroundColor = Color.Transparent,
            ),
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDialogUi() {
    AppTheme {
        AccountClosureDialog(
            onDismissRequest = {},
        ) {
            DialogUi(
                currentUsername = "previewUser",
                isPending = false,
                onConfirm = {},
                onCancel = {},
            )
        }
    }
}
