package org.wordpress.android.ui.bloggingprompts.promptslist

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun BloggingPromptsListScreen(
    onNavigateUp: () -> Unit,
) {
    Scaffold(
            topBar = {
                // TODO extract this logic to a common compose package and fix it to look exactly like the app toolbar
                TopAppBar(
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.onSurface,
                        elevation = 0.dp,
                        title = {
                            Text(stringResource(R.string.blogging_prompts_list_title))
                        },
                        navigationIcon = {
                            IconButton(
                                    onClick = { onNavigateUp() }
                            ) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = stringResource(R.string.navigate_up_desc)
                                )
                            }
                        }
                )
            },
    ) {
        Text("This is just a placeholder for now...")
    }
}

@Preview
@Composable
fun BloggingPromptsListScreenPreview() {
    AppTheme {
        BloggingPromptsListScreen(onNavigateUp = {})
    }
}
