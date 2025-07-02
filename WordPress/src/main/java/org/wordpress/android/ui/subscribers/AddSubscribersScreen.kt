package org.wordpress.android.ui.subscribers

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.validateEmail

@Composable
fun AddSubscribersScreen(
    onSubmit: (List<String>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entry = ""
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {
                    entry = it
                },
                label = {
                    Text(stringResource(id = R.string.subscribers_add_email_hint))
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isValidEntry(entry)) {
                            onSubmit(parseEntry(entry))
                        }
                    }
                ),
                modifier = modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.subscribers_add_disclosure),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }

            Button(
                onClick = { onSubmit(parseEntry(entry)) },
                enabled = entry.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.send))
            }
        }
    }
}

private fun isValidEntry(entry: String): Boolean {
    parseEntry(entry).forEach {
        if (!validateEmail(it)) {
            return false
        }
    }
    return true
}

private fun parseEntry(entry: String): List<String> {
    return entry.split(",").map { it.trim() }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddSubscribersScreenPreview() {
    AppThemeM3 {
        AddSubscribersScreen(
            onSubmit = {},
            onCancel = {}
        )
    }
}
