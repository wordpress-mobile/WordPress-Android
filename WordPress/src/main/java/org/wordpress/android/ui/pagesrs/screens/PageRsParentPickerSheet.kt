package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsParentPickerState

/**
 * Bottom sheet for choosing a page's parent: a "Top level" entry followed by the eligible
 * published pages, filterable with the search field. The current parent shows a check mark.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRsParentPickerSheet(
    state: PageRsParentPickerState,
    onParentSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredCandidates = state.candidates.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.set_parent),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (query.isBlank()) {
                    item(key = "top_level") {
                        ParentCandidateRow(
                            title = stringResource(R.string.top_level),
                            isSelected = state.currentParentId == 0L,
                            onClick = { onParentSelected(0L) }
                        )
                    }
                }
                items(items = filteredCandidates, key = { it.id }) { candidate ->
                    ParentCandidateRow(
                        title = candidate.title.ifBlank {
                            stringResource(R.string.untitled_in_parentheses)
                        },
                        isSelected = state.currentParentId == candidate.id,
                        onClick = { onParentSelected(candidate.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentCandidateRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
