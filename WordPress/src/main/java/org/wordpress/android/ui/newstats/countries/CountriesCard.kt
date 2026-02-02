package org.wordpress.android.ui.newstats.countries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private const val MAP_ASPECT_RATIO = 8f / 5f
private const val LOADING_ITEM_COUNT = 4

@Composable
fun CountriesCard(
    uiState: CountriesCardUiState,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin, vertical = 8.dp)
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(CardCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (uiState) {
            is CountriesCardUiState.Loading -> LoadingContent()
            is CountriesCardUiState.Loaded -> LoadedContent(uiState, onShowAllClick)
            is CountriesCardUiState.Error -> ErrorContent(uiState, onRetry)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(modifier = Modifier.padding(CardPadding)) {
        // Title placeholder
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Map placeholder
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MAP_ASPECT_RATIO)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Legend placeholder
        ShimmerBox(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List items placeholders
        repeat(LOADING_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
        }
    }
}

@Composable
private fun LoadedContent(state: CountriesCardUiState.Loaded, onShowAllClick: () -> Unit) {
    Column(modifier = Modifier.padding(CardPadding)) {
        // Title
        Text(
            text = stringResource(R.string.stats_countries_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.countries.isEmpty()) {
            EmptyContent()
        } else {
            // Map
            CountryMap(
                mapData = state.mapData,
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

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.stats_countries_location_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.stats_countries_views_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Country list (capped at 10 items)
            state.countries.forEachIndexed { index, country ->
                val percentage = if (state.maxViewsForBar > 0) {
                    country.views.toFloat() / state.maxViewsForBar.toFloat()
                } else 0f
                CountryRow(country = country, percentage = percentage)
                if (index < state.countries.lastIndex) {
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
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.stats_no_data_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountryMap(
    mapData: String,
    modifier: Modifier = Modifier
) {
    StatsGeoChartWebView(
        mapData = mapData,
        modifier = modifier
    )
}

@Composable
private fun CountryRow(
    country: CountryItem,
    percentage: Float
) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Background bar representing the percentage
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage)
                .fillMaxHeight()
                .background(barColor)
        )

        // Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag icon
            if (country.flagIconUrl != null) {
                AsyncImage(
                    model = country.flagIconUrl,
                    contentDescription = country.countryName,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Country name
            Text(
                text = country.countryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Views count and change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatStatValue(country.views),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatsChangeIndicator(change = country.change)
            }
        }
    }
}

@Composable
private fun ShowAllFooter(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_show_all),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorContent(
    state: CountriesCardUiState.Error,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        Text(
            text = stringResource(R.string.stats_countries_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.message,
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

// Previews
@Preview(showBackground = true)
@Composable
private fun CountriesCardLoadingPreview() {
    AppThemeM3 {
        CountriesCard(
            uiState = CountriesCardUiState.Loading,
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CountriesCardLoadedPreview() {
    AppThemeM3 {
        CountriesCard(
            uiState = CountriesCardUiState.Loaded(
                countries = listOf(
                    CountryItem("US", "United States", 3464, null, CountryViewChange.Positive(124, 3.7)),
                    CountryItem("ES", "Spain", 556, null, CountryViewChange.Positive(45, 8.8)),
                    CountryItem("GB", "United Kingdom", 522, null, CountryViewChange.Negative(12, 2.2)),
                    CountryItem("CA", "Canada", 485, null, CountryViewChange.NoChange)
                ),
                mapData = "['US',3464],['ES',556],['GB',522],['CA',485]",
                minViews = 485,
                maxViews = 3464,
                maxViewsForBar = 3464,
                hasMoreItems = true
            ),
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CountriesCardErrorPreview() {
    AppThemeM3 {
        CountriesCard(
            uiState = CountriesCardUiState.Error("Failed to load country data"),
            onShowAllClick = {},
            onRetry = {}
        )
    }
}
