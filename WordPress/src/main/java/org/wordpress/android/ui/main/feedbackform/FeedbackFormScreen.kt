package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun FeedbackFormScreen(
    viewModel: FeedbackFormViewModel
) {
    val context = LocalContext.current
    val content: @Composable () -> Unit = @Composable {
        MessageSection(
            messageText = viewModel.messageText.collectAsState()
        ) {
            viewModel.updateMessageText(it)
        }
        SubmitButton {
            viewModel.onSubmitClick(context)
        }
    }
    Screen(
        content = content,
        viewModel = viewModel
    )
}

@Composable
private fun MessageSection(
    messageText: State<String>,
    onMessageChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(
                vertical = vPadding.dp,
                horizontal = hPadding.dp
            )
    ) {
        OutlinedTextField(
            value = messageText.value,
            placeholder = { Text(stringResource(id = R.string.feedback_form_message_hint)) },
            onValueChange = {
                onMessageChanged(it.take(maxChars))
            },
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 180.dp)
        )
    }
}

@Composable
private fun SubmitButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = vPadding.dp,
                horizontal = hPadding.dp
            )
            .testTag("submit_message_button")
    ) {
        Text(
            text = stringResource(R.string.submit).uppercase(),
        )
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
    )
}

@Composable
private fun Screen(
    content: @Composable () -> Unit,
    viewModel: FeedbackFormViewModel,
) {
    val activity = LocalContext.current as? Activity

    AppTheme {
        Scaffold(
            topBar = {
                TopCloseButtonBar(
                    onCloseClick = {
                        viewModel.onCloseClick(activity)
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

private const val hPadding = 18
private const val vPadding = 12
private const val maxChars = 500
