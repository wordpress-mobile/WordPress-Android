package org.wordpress.android.ui.localcontent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListErrorState
import org.wordpress.android.ui.rs.contentlist.ContentListRow
import uniffi.wp_mobile_local_content.WorkingCopyId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingCopyListScreen(
    kind: WorkingCopyKind,
    state: WorkingCopyListUiState,
    onWorkingCopyClick: (WorkingCopyId) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(kind.titleResId)) },
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
                // The read is local and quick, so a placeholder would only flash.
                WorkingCopyListUiState.Loading -> Unit
                is WorkingCopyListUiState.Error -> ContentListErrorState(error = state.message, onRetry = onRetry)
                is WorkingCopyListUiState.Loaded -> if (state.rows.isEmpty()) {
                    ContentListEmptyState(messageResId = kind.emptyMessageResId)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = state.rows, key = { it.id }) { row ->
                            ContentListRow(state = row.row, onClick = { onWorkingCopyClick(row.id) })
                        }
                    }
                }
            }
        }
    }
}
