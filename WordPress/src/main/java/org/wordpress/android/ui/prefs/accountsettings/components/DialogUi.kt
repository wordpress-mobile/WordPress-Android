package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                contentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
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
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.error,
            ),
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDialogUi() {
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
