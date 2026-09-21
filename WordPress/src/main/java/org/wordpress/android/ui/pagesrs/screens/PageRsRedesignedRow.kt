package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsDisplayState
import org.wordpress.android.ui.pagesrs.PageRsListItem
import org.wordpress.android.ui.pagesrs.PageRsMenuAction
import org.wordpress.android.ui.pagesrs.isHeroRow
import org.wordpress.android.ui.pagesrs.toContentListRowUiState
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListHeroRow
import org.wordpress.android.ui.rs.contentlist.ContentListOverflowMenu
import org.wordpress.android.ui.rs.contentlist.ContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.ContentListRow
import org.wordpress.android.ui.rs.contentlist.toContentListMenuActions

/**
 * One row of the redesigned pages list.
 *
 * Renders through the shared [ContentListRow] so the posts and pages lists cannot drift apart, with
 * two things only the pages list has: the hierarchy indent, applied outside the card, and the
 * leading icon that marks the Homepage, Posts page and Site Editor rows.
 */
@Composable
internal fun PageRsRedesignedRow(
    item: PageRsListItem,
    density: ContentListDensity,
    onClick: () -> Unit,
    onMenuAction: (PageRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // The error row still belongs to the pre-redesign presentation: there is no card shape for
    // "this one would not load", and inventing one would be a design decision of its own.
    when (item.page.displayState) {
        PageRsDisplayState.PLACEHOLDER -> ContentListPlaceholderRow(modifier)
        PageRsDisplayState.ERROR -> ErrorItem(modifier)
        PageRsDisplayState.NORMAL,
        PageRsDisplayState.FETCHING_WITH_DATA,
        PageRsDisplayState.FAILED_WITH_DATA -> PageRsContentCard(
            item = item,
            density = density,
            onClick = onClick,
            onMenuAction = onMenuAction,
            modifier = modifier
        )
    }
}

@Composable
private fun PageRsContentCard(
    item: PageRsListItem,
    density: ContentListDensity,
    onClick: () -> Unit,
    onMenuAction: (PageRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val page = item.page
    val state = item.toContentListRowUiState(
        siteEditorTitle = stringResource(R.string.virtual_homepage_title),
        siteEditorSubtitle = stringResource(R.string.virtual_homepage_subtitle)
    )
    val menu: (@Composable () -> Unit)? = if (page.actions.isEmpty()) {
        null
    } else {
        {
            ContentListOverflowMenu(actions = page.actions.toContentListMenuActions(onMenuAction))
        }
    }
    val leading: (@Composable () -> Unit)? = (item as? PageRsListItem.Virtual)?.kind?.let { kind ->
        {
            Icon(
                imageVector = kind.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(LEADING_ICON_SIZE)
            )
        }
    }
    val indent = INDENT_STEP * ((item as? PageRsListItem.Real)?.indentLevel ?: 0)

    // The two shapes are different layouts, not one layout resized, so the density toggle
    // crossfades between them rather than letting the swap snap.
    AnimatedContent(
        targetState = item.isHeroRow(density),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier.padding(start = indent),
        label = "pages list row density"
    ) { isHero ->
        if (isHero) {
            ContentListHeroRow(state = state, onClick = onClick, menu = menu, leading = leading)
        } else {
            ContentListRow(
                state = state,
                onClick = onClick,
                density = density,
                menu = menu,
                leading = leading
            )
        }
    }
}

// Applied outside the card, so it stacks on top of the list's own horizontal padding.
private val INDENT_STEP = 16.dp
private val LEADING_ICON_SIZE = 20.dp
