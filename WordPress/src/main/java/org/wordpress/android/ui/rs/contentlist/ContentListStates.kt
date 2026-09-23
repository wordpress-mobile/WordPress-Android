package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

/**
 * The full-screen states an rs content list can be in instead of showing rows, plus the spinner it
 * appends while paging. Shared by the posts, pages and comments lists, which draw all of them
 * identically.
 */
@Composable
fun ContentListErrorState(
    error: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ContentListMessageColumn(modifier) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(TEXT_GAP))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(BUTTON_GAP))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

/**
 * [actionLabelResId] and [onAction] go together: the posts list offers to create a post from its
 * empty state and the pages list offers nothing, so both are null there.
 */
@Composable
fun ContentListEmptyState(
    @StringRes messageResId: Int,
    modifier: Modifier = Modifier,
    @StringRes actionLabelResId: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    ContentListMessageColumn(modifier) {
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabelResId != null && onAction != null) {
            Spacer(modifier = Modifier.height(BUTTON_GAP))
            Button(onClick = onAction) {
                Text(text = stringResource(actionLabelResId))
            }
        }
    }
}

/**
 * A screenful of [placeholder] rows while a tab's first page loads. The row is the caller's because
 * a comment's skeleton leads with an avatar where a post's and a page's lead with text.
 */
@Composable
fun ContentListShimmer(placeholder: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(SHIMMER_ITEM_COUNT) { placeholder() }
    }
}

/** The post and page skeleton, in whichever row style is showing. */
@Composable
fun ContentListShimmer(isRedesignEnabled: Boolean) {
    ContentListShimmer {
        if (isRedesignEnabled) ContentListPlaceholderRow() else LegacyContentListPlaceholderRow()
    }
}

/** The spinner a list appends while it pages in more rows. */
fun LazyListScope.contentListLoadingMoreItem() {
    item(key = "loading_more") {
        Box(
            modifier = Modifier
                .fillParentMaxWidth()
                .padding(MESSAGE_PADDING),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE),
                strokeWidth = SPINNER_STROKE
            )
        }
    }
}

/** Scrollable so the message still reaches the user when the keyboard is up on a short screen. */
@Composable
private fun ContentListMessageColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(COLUMN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
}

private val COLUMN_PADDING = 32.dp
private val MESSAGE_PADDING = 16.dp
private val TEXT_GAP = 8.dp
private val BUTTON_GAP = 16.dp
private val SPINNER_SIZE = 24.dp
private val SPINNER_STROKE = 2.dp

/** Roughly a screenful, so the skeleton fills the list rather than leaving a gap below it. */
private const val SHIMMER_ITEM_COUNT = 8
