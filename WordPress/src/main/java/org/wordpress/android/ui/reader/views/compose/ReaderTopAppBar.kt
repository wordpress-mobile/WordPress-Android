package org.wordpress.android.ui.reader.views.compose

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.dropdown.JetpackDropdownMenu
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.horizontalFadingEdges
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterChipGroup
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiString

private const val ANIM_DURATION = 200

@Composable
fun ReaderTopAppBar(
    topBarUiState: ReaderViewModel.TopBarUiState,
    onMenuItemClick: (MenuElementData.Item.Single) -> Unit,
    onFilterClick: (ReaderFilterType) -> Unit,
    onClearFilterClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    var selectedItem by remember { mutableStateOf(topBarUiState.selectedItem) }
    var isFilterShown by remember { mutableStateOf(topBarUiState.filterUiState != null) }
    var latestFilterState by remember { mutableStateOf(topBarUiState.filterUiState) }

    // Coordinate filter enter and exit animations with the dropdown menu (delays are required for a nice experience)
    val shouldShowFilter = topBarUiState.filterUiState != null
    LaunchedEffect(shouldShowFilter, topBarUiState.selectedItem) {
        if (isFilterShown != shouldShowFilter) {
            isFilterShown = shouldShowFilter
            if (!shouldShowFilter) delay(ANIM_DURATION.toLong())
        }
        selectedItem = topBarUiState.selectedItem
    }

    // Update filter state when it changes to non-null value. We need to keep it non-null so that the exit animation
    // works properly.
    if (topBarUiState.filterUiState != null) {
        latestFilterState = topBarUiState.filterUiState
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(
                start = Margin.ExtraLarge.value,
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .horizontalFadingEdges(scrollState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JetpackDropdownMenu(
                    selectedItem = selectedItem,
                    menuItems = topBarUiState.menuItems,
                    onSingleItemClick = onMenuItemClick,
                    contentSizeAnimation = tween(ANIM_DURATION),
                )

                AnimatedVisibility(
                    visible = isFilterShown,
                    enter = fadeIn(tween(delayMillis = ANIM_DURATION)) +
                            slideInVertically(tween(delayMillis = ANIM_DURATION)) { it / 2 },
                    exit = fadeOut(tween(ANIM_DURATION)) +
                            slideOutVertically(tween(ANIM_DURATION)) { it / 2 },
                ) {
                    latestFilterState?.let { filterUiState ->
                        Filter(
                            filterUiState = filterUiState,
                            onFilterClick = onFilterClick,
                            onClearFilterClick = onClearFilterClick,
                            modifier = Modifier
                                // use padding instead of Spacer for nicer animation
                                .padding(start = Margin.Medium.value),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(Margin.ExtraLarge.value))
        IconButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = { onSearchClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_magnifying_glass_16dp),
                contentDescription = stringResource(
                    R.string.reader_search_content_description
                ),
                tint = MaterialTheme.colors.onSurface,
            )
        }
    }
}

@Composable
private fun Filter(
    filterUiState: ReaderViewModel.TopBarUiState.FilterUiState,
    onFilterClick: (ReaderFilterType) -> Unit,
    onClearFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderFilterChipGroup(
        modifier = modifier,
        selectedItem = filterUiState.selectedItem,
        followedBlogsCount = filterUiState.followedBlogsCount,
        followedTagsCount = filterUiState.followedTagsCount,
        onFilterClick = onFilterClick,
        onSelectedItemClick = onClearFilterClick,
        onSelectedItemDismissClick = onClearFilterClick,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderScreenPreview() {
    val menuItems = mutableListOf<MenuElementData>(
        MenuElementData.Item.Single(
            id = "discover",
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_discover),
            leadingIcon = R.drawable.ic_reader_discover_24dp,
        ),
        MenuElementData.Item.Single(
            id = "subscriptions",
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_subscriptions),
            leadingIcon = R.drawable.ic_reader_subscriptions_24dp,
        ),
        MenuElementData.Item.Single(
            id = "saved",
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_saved),
            leadingIcon = R.drawable.ic_reader_saved_24dp,
        ),
        MenuElementData.Item.Single(
            id = "liked",
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_liked),
            leadingIcon = R.drawable.ic_reader_liked_24dp,
        ),
    )

    var topBarUiState by remember {
        mutableStateOf(
            ReaderViewModel.TopBarUiState(
                menuItems = menuItems,
                selectedItem = menuItems.first() as MenuElementData.Item.Single,
            )
        )
    }

    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            ReaderTopAppBar(
                topBarUiState = topBarUiState,
                onMenuItemClick = {
                    topBarUiState = topBarUiState.copy(
                        selectedItem = it
                    )
                },
                onFilterClick = {},
                onClearFilterClick = {},
                onSearchClick = {},
            )
        }
    }
}
