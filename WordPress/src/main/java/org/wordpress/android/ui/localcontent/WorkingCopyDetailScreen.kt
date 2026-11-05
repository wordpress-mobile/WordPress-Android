package org.wordpress.android.ui.localcontent

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingCopyDetailScreen(
    state: WorkingCopyDetailUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state is WorkingCopyDetailUiState.Loaded) {
                        Text(
                            text = state.title.ifBlank { stringResource(R.string.untitled_in_parentheses) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (state) {
                WorkingCopyDetailUiState.Loading -> Unit
                WorkingCopyDetailUiState.NotFound ->
                    ContentListEmptyState(messageResId = R.string.local_content_working_copy_not_found)
                is WorkingCopyDetailUiState.Error -> ContentListErrorState(error = state.message, onRetry = null)
                is WorkingCopyDetailUiState.Loaded -> FieldList(state.sections)
            }
        }
    }
}

@Composable
private fun FieldList(sections: List<WorkingCopyFieldSection>) {
    // Selectable, so a value can be copied into a bug report or compared with the server's.
    SelectionContainer {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            sections.forEach { section ->
                item(key = section.title) { SectionHeader(section.title) }
                items(items = section.fields, key = { "${section.title}.${it.name}" }) { field ->
                    FieldRow(field)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = Margin.ExtraLarge.value,
                end = Margin.ExtraLarge.value,
                top = Margin.ExtraLarge.value,
                bottom = Margin.Small.value
            )
        )
    }
}

@Composable
private fun FieldRow(field: WorkingCopyField) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.Medium.value)
    ) {
        Text(
            text = field.name,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = field.value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun WorkingCopyDetailScreenPreview() {
    AppThemeM3 {
        WorkingCopyDetailScreen(
            state = WorkingCopyDetailUiState.Loaded(
                title = "Hello",
                sections = listOf(
                    WorkingCopyFieldSection(
                        title = "remoteTarget",
                        fields = listOf(
                            WorkingCopyField("site.url", "https://example.com/"),
                            WorkingCopyField("remotePost", "null")
                        )
                    ),
                    WorkingCopyFieldSection(
                        title = "fields.common",
                        fields = listOf(
                            WorkingCopyField("title", "Hello"),
                            WorkingCopyField("status", "Draft")
                        )
                    )
                )
            ),
            onNavigateBack = {}
        )
    }
}
