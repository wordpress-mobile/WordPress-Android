package org.wordpress.android.ui.subscribers

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    showProgress: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    var entry by remember { mutableStateOf("") }
    var isValidEntry by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = entry,
            onValueChange = {
                entry = it
                isValidEntry = isValidEntry(it)
            },
            label = {
                Text(stringResource(id = R.string.subscribers_add_email_hint))
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isValidEntry) {
                        onSubmit(parseEntry(entry))
                    }
                }
            ),
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.subscribers_add_disclosure),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (showProgress.value) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
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
                    enabled = isValidEntry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.send))
                }
            }
        }
    }
}

@Suppress("ReturnCount")
private fun isValidEntry(entry: String): Boolean {
    if (entry.isEmpty()) {
        return false
    }
    parseEntry(entry).forEach {
        if (!validateEmail(it)) {
            return false
        }
    }
    return true
}

private fun parseEntry(entry: String): List<String> {
    return entry.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddSubscribersScreenPreview() {
    AppThemeM3 {
        AddSubscribersScreen(
            onSubmit = {},
            onCancel = {},
            showProgress = remember { mutableStateOf(false) }
        )
    }
}
