package org.wordpress.android.ui.main.feedbackform

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun FeedbackFormScreen(
    messageText: State<String>?,
    isProgressShowing: State<Boolean?>,
    onMessageChanged: (String) -> Unit,
    onSubmitClick: (context: Context) -> Unit,
    onCloseClick: (context: Context) -> Unit
) {
    val context = LocalContext.current
    val message = messageText?.value ?: ""
    val content: @Composable () -> Unit = @Composable {
        MessageSection(
            messageText = messageText?.value,
            onMessageChanged = {
                onMessageChanged(it)
            },
        )
        SubmitButton(
            isEnabled = message.isNotEmpty(),
            isProgressShowing = isProgressShowing.value,
            onClick = {
                onSubmitClick(context)
            }
        )
    }
    Screen(
        content = content,
        onCloseClick = { onCloseClick(context) }
    )
}

@Composable
private fun MessageSection(
    messageText: String?,
    onMessageChanged: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(
                vertical = V_PADDING.dp,
                horizontal = H_PADDING.dp
            )
    ) {
        OutlinedTextField(
            value = messageText ?: "",
            placeholder = {
                Text(stringResource(id = R.string.feedback_form_message_hint))
            },
            onValueChange = {
                onMessageChanged(it.take(MAX_CHARS))
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 180.dp)
        )
    }
}

@Composable
private fun SubmitButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isProgressShowing: Boolean?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = V_PADDING.dp,
                horizontal = H_PADDING.dp
            ),
    ) {
        if (isProgressShowing == true) {
            CircularProgressIndicator()
        } else {
            Button(
                enabled = isEnabled,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.submit).uppercase(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopCloseButtonBar(
    onCloseClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.feedback_form_title)) },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun Screen(
    content: @Composable () -> Unit,
    onCloseClick: (context: Context) -> Unit
) {
    val context = LocalContext.current

    AppTheme {
        Scaffold(
            topBar = {
                TopCloseButtonBar(
                    onCloseClick = {
                        onCloseClick(context)
                    }
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FeedbackFormScreenPreview() {
    val content: @Composable () -> Unit = @Composable {
        MessageSection(
            messageText = null,
            onMessageChanged = {},
        )
        SubmitButton(
            isEnabled = true,
            isProgressShowing = false,
            onClick = { }
        )
    }
    Screen(
        content = content,
        onCloseClick = {},
    )
}

private const val H_PADDING = 18
private const val V_PADDING = 12
private const val MAX_CHARS = 500 // matches iOS limit
