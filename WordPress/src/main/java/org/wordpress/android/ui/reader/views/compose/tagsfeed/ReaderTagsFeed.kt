package org.wordpress.android.ui.reader.views.compose.tagsfeed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun ReaderTagsFeed(uiState: UiState) {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            when (uiState) {
                is UiState.Loaded -> Loaded(uiState.tags)
                is UiState.Loading -> {

                }
                is UiState.Error -> {

                }
            }
        }
    }
}

@Composable
private fun Loaded(items: List<TagsFeedItem>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
    }
}


// TODO move to VM
sealed class UiState {
    // TODO Loaded parameters
    data class Loaded(
        val tags: List<ReaderTag>,
    ) : UiState()

    object Loading : UiState()
}

sealed class TagsFeedItem {
    data class Success(
        val tag: ReaderTag,
        val posts: List<ReaderPost>
    )

    data class Error(
        val tag: ReaderTag,
    )
}
