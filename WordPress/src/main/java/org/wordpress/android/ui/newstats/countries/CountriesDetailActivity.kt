package org.wordpress.android.ui.newstats.countries

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getParcelableArrayListCompat
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

private const val EXTRA_COUNTRIES = "extra_countries"
private const val EXTRA_MAP_DATA = "extra_map_data"
private const val EXTRA_MIN_VIEWS = "extra_min_views"
private const val EXTRA_MAX_VIEWS = "extra_max_views"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT = "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"
private const val MAP_ASPECT_RATIO = 8f / 5f

@AndroidEntryPoint
class CountriesDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val countries = intent.extras
            ?.getParcelableArrayListCompat<CountriesDetailItem>(EXTRA_COUNTRIES)
            ?: arrayListOf()
        val mapData = intent.getStringExtra(EXTRA_MAP_DATA) ?: ""
        val minViews = intent.getLongExtra(EXTRA_MIN_VIEWS, 0L)
        val maxViews = intent.getLongExtra(EXTRA_MAX_VIEWS, 0L)
        val totalViews = intent.getLongExtra(EXTRA_TOTAL_VIEWS, 0L)
        val totalViewsChange = intent.getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L)
        val totalViewsChangePercent = intent.getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0)
        val dateRange = intent.getStringExtra(EXTRA_DATE_RANGE) ?: ""
        // Calculate maxViewsForBar once (list is sorted by views descending)
        val maxViewsForBar = countries.firstOrNull()?.views ?: 1L

        setContent {
            AppThemeM3 {
                CountriesDetailScreen(
                    countries = countries,
                    mapData = mapData,
                    minViews = minViews,
                    maxViews = maxViews,
                    maxViewsForBar = maxViewsForBar,
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent,
                    dateRange = dateRange,
                    onBackPressed = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun start(
            context: Context,
            countries: List<CountryItem>,
            mapData: String,
            minViews: Long,
            maxViews: Long,
            totalViews: Long,
            totalViewsChange: Long,
            totalViewsChangePercent: Double,
            dateRange: String
        ) {
            val detailItems = countries.map { country ->
                CountriesDetailItem(
                    countryCode = country.countryCode,
                    countryName = country.countryName,
                    views = country.views,
                    flagIconUrl = country.flagIconUrl,
                    change = country.change
                )
            }
            val intent = Intent(context, CountriesDetailActivity::class.java).apply {
                putExtra(EXTRA_COUNTRIES, ArrayList(detailItems))
                putExtra(EXTRA_MAP_DATA, mapData)
                putExtra(EXTRA_MIN_VIEWS, minViews)
                putExtra(EXTRA_MAX_VIEWS, maxViews)
                putExtra(EXTRA_TOTAL_VIEWS, totalViews)
                putExtra(EXTRA_TOTAL_VIEWS_CHANGE, totalViewsChange)
                putExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, totalViewsChangePercent)
                putExtra(EXTRA_DATE_RANGE, dateRange)
            }
            context.startActivity(intent)
        }
    }
}

@Parcelize
data class CountriesDetailItem(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?,
    val change: CountryViewChange = CountryViewChange.NoChange
) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountriesDetailScreen(
    countries: List<CountriesDetailItem>,
    mapData: String,
    minViews: Long,
    maxViews: Long,
    maxViewsForBar: Long,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.stats_countries_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Summary card
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Map
                CountryMap(
                    mapData = mapData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MAP_ASPECT_RATIO)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                StatsMapLegend(minViews = minViews, maxViews = maxViews)
                Spacer(modifier = Modifier.height(16.dp))
            }


            item {
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
            }

            itemsIndexed(countries) { index, country ->
                val percentage = if (maxViewsForBar > 0) {
                    country.views.toFloat() / maxViewsForBar.toFloat()
                } else 0f
                DetailCountryRow(
                    position = index + 1,
                    country = country,
                    percentage = percentage
                )
                if (index < countries.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
private fun DetailCountryRow(
    position: Int,
    country: CountriesDetailItem,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

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

@Preview(showBackground = true)
@Composable
private fun CountriesDetailScreenPreview() {
    AppThemeM3 {
        CountriesDetailScreen(
            countries = listOf(
                CountriesDetailItem("US", "United States", 3464, null, CountryViewChange.Positive(124, 3.7)),
                CountriesDetailItem("ES", "Spain", 556, null, CountryViewChange.Positive(45, 8.8)),
                CountriesDetailItem("GB", "United Kingdom", 522, null, CountryViewChange.Negative(12, 2.2)),
                CountriesDetailItem("CA", "Canada", 485, null, CountryViewChange.Positive(33, 7.3)),
                CountriesDetailItem("DE", "Germany", 412, null, CountryViewChange.NoChange),
                CountriesDetailItem("FR", "France", 387, null, CountryViewChange.Negative(8, 2.0)),
                CountriesDetailItem("AU", "Australia", 298, null, CountryViewChange.Positive(21, 7.6)),
                CountriesDetailItem("BR", "Brazil", 245, null, CountryViewChange.Positive(15, 6.5)),
                CountriesDetailItem("IN", "India", 201, null, CountryViewChange.Negative(5, 2.4)),
                CountriesDetailItem("MX", "Mexico", 156, null, CountryViewChange.Positive(12, 8.3))
            ),
            mapData = "['US',3464],['ES',556],['GB',522],['CA',485]",
            minViews = 156,
            maxViews = 3464,
            maxViewsForBar = 3464,
            totalViews = 6726,
            totalViewsChange = 225,
            totalViewsChangePercent = 3.5,
            dateRange = "Last 7 days",
            onBackPressed = {}
        )
    }
}
