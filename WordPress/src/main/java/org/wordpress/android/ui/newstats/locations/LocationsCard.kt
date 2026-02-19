package org.wordpress.android.ui.newstats.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListItem
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
private const val MAP_ASPECT_RATIO = 8f / 5f
private const val LOADING_ITEM_COUNT = 4

@Composable
fun LocationsCard(
    uiState: LocationsCardUiState,
    selectedLocationType: LocationType,
    onLocationTypeChanged: (LocationType) -> Unit,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is LocationsCardUiState.Loading -> LoadingContent(
                selectedLocationType = selectedLocationType,
                onLocationTypeChanged = onLocationTypeChanged,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            is LocationsCardUiState.Loaded -> LoadedContent(
                uiState, selectedLocationType, onLocationTypeChanged,
                onShowAllClick, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop,
                onMoveDown, onMoveToBottom
            )
            is LocationsCardUiState.Error -> ErrorContent(
                uiState = uiState,
                selectedLocationType = selectedLocationType,
                onLocationTypeChanged = onLocationTypeChanged,
                onRetry = onRetry,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
        }
    }
}

@Composable
private fun LoadingContent(
    selectedLocationType: LocationType,
    onLocationTypeChanged: (LocationType) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val titleResId = when (selectedLocationType) {
        LocationType.COUNTRIES -> R.string.stats_countries_title
        LocationType.REGIONS -> R.string.stats_regions_title
        LocationType.CITIES -> R.string.stats_cities_title
    }
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = titleResId,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Location type selector
        LocationTypeSelector(
            selectedType = selectedLocationType,
            onTypeSelected = onLocationTypeChanged
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Map placeholder
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MAP_ASPECT_RATIO)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Legend placeholder
        ShimmerBox(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        StatsListHeader(
            leftHeaderResId =
                R.string.stats_countries_location_header
        )
        Spacer(modifier = Modifier.height(8.dp))

        // List items placeholders
        repeat(LOADING_ITEM_COUNT) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 12.dp,
                        horizontal = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp)
                )
            }
            if (index < LOADING_ITEM_COUNT - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: LocationsCardUiState.Loaded,
    selectedLocationType: LocationType,
    onLocationTypeChanged: (LocationType) -> Unit,
    onShowAllClick: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val titleResId = when (selectedLocationType) {
        LocationType.COUNTRIES -> R.string.stats_countries_title
        LocationType.REGIONS -> R.string.stats_regions_title
        LocationType.CITIES -> R.string.stats_cities_title
    }
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = titleResId,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Location type selector
        LocationTypeSelector(
            selectedType = selectedLocationType,
            onTypeSelected = onLocationTypeChanged
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            StatsCardEmptyContent()
        } else {
            // Map
            CountryMap(
                mapData = state.mapData,
                useMarkers =
                    selectedLocationType == LocationType.CITIES,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(MAP_ASPECT_RATIO)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            StatsMapLegend(
                minViews = state.minViews,
                maxViews = state.maxViews
            )
            Spacer(modifier = Modifier.height(16.dp))

            StatsListHeader(
                leftHeaderResId =
                    R.string.stats_countries_location_header
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Location list (capped at 10 items)
            state.items.forEachIndexed { index, item ->
                val percentage = if (state.maxViewsForBar > 0) {
                    item.views.toFloat() /
                        state.maxViewsForBar.toFloat()
                } else 0f
                LocationRow(
                    item = item,
                    percentage = percentage
                )
                if (index < state.items.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Show All footer
            Spacer(modifier = Modifier.height(12.dp))
            ShowAllFooter(onClick = onShowAllClick)
        }
    }
}

@Composable
private fun ErrorContent(
    uiState: LocationsCardUiState.Error,
    selectedLocationType: LocationType,
    onLocationTypeChanged: (LocationType) -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val titleResId = when (selectedLocationType) {
        LocationType.COUNTRIES -> R.string.stats_countries_title
        LocationType.REGIONS -> R.string.stats_regions_title
        LocationType.CITIES -> R.string.stats_cities_title
    }
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = titleResId,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Location type selector
        LocationTypeSelector(
            selectedType = selectedLocationType,
            onTypeSelected = onLocationTypeChanged
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationTypeSelector(
    selectedType: LocationType,
    onTypeSelected: (LocationType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        LocationType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = LocationType.entries.size
                ),
                onClick = { onTypeSelected(type) },
                selected = type == selectedType
            ) {
                Text(text = stringResource(type.labelResId))
            }
        }
    }
}

@Composable
private fun CountryMap(
    mapData: String,
    modifier: Modifier = Modifier,
    useMarkers: Boolean = false
) {
    StatsGeoChartWebView(
        mapData = mapData,
        useMarkers = useMarkers,
        modifier = modifier
    )
}

@Composable
private fun LocationRow(
    item: LocationItem,
    percentage: Float
) {
    StatsListItem(
        percentage = percentage,
        name = item.name,
        views = item.views,
        change = item.change,
        icon = {
            CountryFlag(
                flagIconUrl = item.flagIconUrl,
                countryName = item.name
            )
        }
    )
}

@Composable
fun CountryFlag(
    flagIconUrl: String?,
    countryName: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    if (flagIconUrl != null) {
        AsyncImage(
            model = flagIconUrl,
            contentDescription = countryName,
            modifier = modifier.size(size)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun LocationsCardLoadingPreview() {
    AppThemeM3 {
        LocationsCard(
            uiState = LocationsCardUiState.Loading,
            selectedLocationType = LocationType.COUNTRIES,
            onLocationTypeChanged = {},
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationsCardLoadedPreview() {
    AppThemeM3 {
        LocationsCard(
            uiState = LocationsCardUiState.Loaded(
                items = listOf(
                    LocationItem(
                        "US", "United States", 3464, null,
                        StatsViewChange.Positive(124, 3.7)
                    ),
                    LocationItem(
                        "ES", "Spain", 556, null,
                        StatsViewChange.Positive(45, 8.8)
                    ),
                    LocationItem(
                        "GB", "United Kingdom", 522, null,
                        StatsViewChange.Negative(12, 2.2)
                    ),
                    LocationItem(
                        "CA", "Canada", 485, null,
                        StatsViewChange.NoChange
                    )
                ),
                mapData = "['US',3464],['ES',556]," +
                    "['GB',522],['CA',485]",
                minViews = 485,
                maxViews = 3464,
                maxViewsForBar = 3464,
                hasMoreItems = true
            ),
            selectedLocationType = LocationType.COUNTRIES,
            onLocationTypeChanged = {},
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationsCardRegionsPreview() {
    AppThemeM3 {
        LocationsCard(
            uiState = LocationsCardUiState.Loaded(
                items = listOf(
                    LocationItem(
                        "CA", "California", 1234, null,
                        StatsViewChange.Positive(100, 8.8)
                    ),
                    LocationItem(
                        "TX", "Texas", 890, null,
                        StatsViewChange.Positive(45, 5.3)
                    )
                ),
                mapData = "['California',1234],['Texas',890]",
                minViews = 890,
                maxViews = 1234,
                maxViewsForBar = 1234,
                hasMoreItems = false
            ),
            selectedLocationType = LocationType.REGIONS,
            onLocationTypeChanged = {},
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationsCardErrorPreview() {
    AppThemeM3 {
        LocationsCard(
            uiState = LocationsCardUiState.Error(
                "Failed to load country data"
            ),
            selectedLocationType = LocationType.COUNTRIES,
            onLocationTypeChanged = {},
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
