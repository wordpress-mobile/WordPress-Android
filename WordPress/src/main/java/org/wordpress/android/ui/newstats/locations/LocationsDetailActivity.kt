package org.wordpress.android.ui.newstats.locations

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.components.StatsDetailListItem
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsSummaryCard
import org.wordpress.android.util.extensions.getParcelableArrayListCompat

private const val EXTRA_COUNTRIES = "extra_countries"
private const val EXTRA_MAP_DATA = "extra_map_data"
private const val EXTRA_MIN_VIEWS = "extra_min_views"
private const val EXTRA_MAX_VIEWS = "extra_max_views"
private const val EXTRA_TOTAL_VIEWS = "extra_total_views"
private const val EXTRA_TOTAL_VIEWS_CHANGE = "extra_total_views_change"
private const val EXTRA_TOTAL_VIEWS_CHANGE_PERCENT =
    "extra_total_views_change_percent"
private const val EXTRA_DATE_RANGE = "extra_date_range"
private const val EXTRA_LOCATION_TYPE = "extra_location_type"
private const val MAP_ASPECT_RATIO = 8f / 5f

@AndroidEntryPoint
class LocationsDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val countries = intent.extras
            ?.getParcelableArrayListCompat<LocationItem>(
                EXTRA_COUNTRIES
            ) ?: arrayListOf()
        val mapData = intent.getStringExtra(EXTRA_MAP_DATA) ?: ""
        val minViews = intent.getLongExtra(EXTRA_MIN_VIEWS, 0L)
        val maxViews = intent.getLongExtra(EXTRA_MAX_VIEWS, 0L)
        val totalViews = intent.getLongExtra(EXTRA_TOTAL_VIEWS, 0L)
        val totalViewsChange =
            intent.getLongExtra(EXTRA_TOTAL_VIEWS_CHANGE, 0L)
        val totalViewsChangePercent =
            intent.getDoubleExtra(EXTRA_TOTAL_VIEWS_CHANGE_PERCENT, 0.0)
        val dateRange = intent.getStringExtra(EXTRA_DATE_RANGE) ?: ""
        val locationTypeName =
            intent.getStringExtra(EXTRA_LOCATION_TYPE)
        val locationType = locationTypeName?.let {
            runCatching { LocationType.valueOf(it) }
                .getOrDefault(LocationType.COUNTRIES)
        } ?: LocationType.COUNTRIES
        val maxViewsForBar =
            countries.firstOrNull()?.views ?: 0L

        setContent {
            AppThemeM3 {
                LocationsDetailScreen(
                    countries = countries,
                    mapData = mapData,
                    minViews = minViews,
                    maxViews = maxViews,
                    maxViewsForBar = maxViewsForBar,
                    totalViews = totalViews,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent,
                    dateRange = dateRange,
                    locationType = locationType,
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(
            context: Context,
            detailData: LocationsDetailData
        ) {
            val intent = Intent(
                context, LocationsDetailActivity::class.java
            ).apply {
                putExtra(
                    EXTRA_COUNTRIES,
                    ArrayList(detailData.items)
                )
                putExtra(EXTRA_MAP_DATA, detailData.mapData)
                putExtra(EXTRA_MIN_VIEWS, detailData.minViews)
                putExtra(EXTRA_MAX_VIEWS, detailData.maxViews)
                putExtra(EXTRA_TOTAL_VIEWS, detailData.totalViews)
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE,
                    detailData.totalViewsChange
                )
                putExtra(
                    EXTRA_TOTAL_VIEWS_CHANGE_PERCENT,
                    detailData.totalViewsChangePercent
                )
                putExtra(EXTRA_DATE_RANGE, detailData.dateRange)
                putExtra(
                    EXTRA_LOCATION_TYPE,
                    detailData.locationType.name
                )
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationsDetailScreen(
    countries: List<LocationItem>,
    mapData: String,
    minViews: Long,
    maxViews: Long,
    maxViewsForBar: Long,
    totalViews: Long,
    totalViewsChange: Long,
    totalViewsChangePercent: Double,
    dateRange: String,
    locationType: LocationType,
    onBackPressed: () -> Unit
) {
    val titleResId = when (locationType) {
        LocationType.COUNTRIES -> R.string.stats_countries_title
        LocationType.REGIONS -> R.string.stats_regions_title
        LocationType.CITIES -> R.string.stats_cities_title
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(titleResId))
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                stringResource(R.string.back)
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
                StatsSummaryCard(
                    totalViews = totalViews,
                    dateRange = dateRange,
                    totalViewsChange = totalViewsChange,
                    totalViewsChangePercent = totalViewsChangePercent
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Map
                StatsGeoChartWebView(
                    mapData = mapData,
                    useMarkers =
                        locationType == LocationType.CITIES,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MAP_ASPECT_RATIO)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                StatsMapLegend(
                    minViews = minViews,
                    maxViews = maxViews
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                StatsListHeader(
                    leftHeaderResId =
                        R.string.stats_countries_location_header
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(countries) { index, country ->
                val percentage = if (maxViewsForBar > 0) {
                    country.views.toFloat() /
                        maxViewsForBar.toFloat()
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
private fun DetailCountryRow(
    position: Int,
    country: LocationItem,
    percentage: Float
) {
    StatsDetailListItem(
        position = position,
        percentage = percentage,
        name = country.name,
        views = country.views,
        change = country.change,
        icon = {
            CountryFlag(
                flagIconUrl = country.flagIconUrl,
                countryName = country.name
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun LocationsDetailScreenPreview() {
    AppThemeM3 {
        LocationsDetailScreen(
            countries = listOf(
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
                    StatsViewChange.Positive(33, 7.3)
                ),
                LocationItem(
                    "DE", "Germany", 412, null,
                    StatsViewChange.NoChange
                )
            ),
            mapData = "['US',3464],['ES',556]," +
                "['GB',522],['CA',485]",
            minViews = 156,
            maxViews = 3464,
            maxViewsForBar = 3464,
            totalViews = 6726,
            totalViewsChange = 225,
            totalViewsChangePercent = 3.5,
            dateRange = "Last 7 days",
            locationType = LocationType.COUNTRIES,
            onBackPressed = {}
        )
    }
}
