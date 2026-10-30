package org.wordpress.android.ui.reader.views.compose.subfilter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.views.compose.ReaderSheetHandle
import org.wordpress.android.ui.reader.views.compose.ReaderSiteRow
import org.wordpress.android.util.UrlUtils

/**
 * The Reader's blog or tag filter sheet: the followed sites or tags, or [emptyState] when there are none
 */
@Composable
@Suppress("LongParameterList")
fun SubfilterSheet(
    title: String,
    items: List<SubfilterListItem>,
    currentSubfilter: SubfilterListItem?,
    emptyState: VisibleEmptyUiState?,
    unseenCountText: (Site) -> String?,
    onManageClick: () -> Unit,
    onEmptyStateAction: (ActionType) -> Unit
) {
    val sheetDescription = stringResource(R.string.reader_bottom_sheet_select_content_description)
    Column(modifier = Modifier.semantics { contentDescription = sheetDescription }) {
        ReaderSheetHandle()
        if (emptyState != null) {
            SubfilterEmptyState(emptyState, onEmptyStateAction)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Margin.ExtraLarge.value, end = Margin.Medium.value)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManageClick) {
                    Text(stringResource(R.string.manage))
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = Margin.ExtraMediumLarge.value),
                // lets the list scroll inside the sheet instead of dragging it
                modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                items(items) { item ->
                    val isSelected = item.isSameItem(currentSubfilter)
                    when (item) {
                        is Site -> SubfilterSiteRow(item, isSelected, unseenCountText(item))
                        is Tag -> SubfilterTagRow(item, isSelected)
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SubfilterSiteRow(site: Site, isSelected: Boolean, unseenCount: String?) {
    val blog = site.blog
    val host = remember(blog.url, blog.feedUrl) {
        when {
            blog.hasUrl() -> UrlUtils.getHost(blog.url)
            blog.hasFeedUrl() -> UrlUtils.getHost(blog.feedUrl)
            else -> ""
        }
    }
    ReaderSiteRow(
        title = uiStringText(site.label),
        subtitle = host,
        iconUrl = blog.imageUrl.takeIf { blog.hasImageUrl() },
        onClick = { site.onClickAction(site) },
        isSelected = isSelected,
        badge = unseenCount
    )
}

@Composable
private fun SubfilterTagRow(tag: Tag, isSelected: Boolean) {
    Text(
        text = uiStringText(tag.label),
        style = MaterialTheme.typography.titleMedium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { tag.onClickAction(tag) }
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.ExtraLarge.value)
    )
}

/**
 * What the filter sheet shows when nothing is followed, also used by the login-required sheet
 */
@Composable
fun SubfilterEmptyState(state: VisibleEmptyUiState, onAction: (ActionType) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Margin.ExtraMediumLarge.value)
    ) {
        state.title?.let { title ->
            Text(
                text = uiStringText(title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = Margin.Medium.value)
            )
        }
        Text(
            text = uiStringText(state.text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Margin.ExtraMediumLarge.value)
        )
        state.primaryButton?.let { button ->
            Button(
                onClick = { onAction(button.action) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Margin.Small.value)
            ) {
                Text(uiStringText(button.text))
            }
        }
        state.secondaryButton?.let { button ->
            OutlinedButton(onClick = { onAction(button.action) }, modifier = Modifier.fillMaxWidth()) {
                Text(uiStringText(button.text))
            }
        }
    }
}
