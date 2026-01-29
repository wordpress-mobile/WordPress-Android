package org.wordpress.android.ui.newstats.countries

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.util.extensions.getSerializableCompat
import java.io.Serializable

private const val EXTRA_COUNTRIES = "extra_countries"
private const val EXTRA_MAP_DATA = "extra_map_data"
private const val EXTRA_MIN_VIEWS = "extra_min_views"
private const val EXTRA_MAX_VIEWS = "extra_max_views"
private const val MAP_ASPECT_RATIO = 8f / 5f

@AndroidEntryPoint
class CountriesDetailActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("UNCHECKED_CAST")
        val countries = intent.extras
            ?.getSerializableCompat<ArrayList<CountriesDetailItem>>(EXTRA_COUNTRIES)
            ?: arrayListOf()
        val mapData = intent.getStringExtra(EXTRA_MAP_DATA) ?: ""
        val minViews = intent.getLongExtra(EXTRA_MIN_VIEWS, 0L)
        val maxViews = intent.getLongExtra(EXTRA_MAX_VIEWS, 0L)

        setContent {
            AppThemeM3 {
                CountriesDetailScreen(
                    countries = countries,
                    mapData = mapData,
                    minViews = minViews,
                    maxViews = maxViews,
                    onBackPressed = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(
            context: Context,
            countries: List<CountryItem>,
            mapData: String,
            minViews: Long,
            maxViews: Long
        ) {
            val detailItems = countries.map { country ->
                CountriesDetailItem(
                    countryCode = country.countryCode,
                    countryName = country.countryName,
                    views = country.views,
                    flagIconUrl = country.flagIconUrl
                )
            }
            val intent = Intent(context, CountriesDetailActivity::class.java).apply {
                putExtra(EXTRA_COUNTRIES, ArrayList(detailItems))
                putExtra(EXTRA_MAP_DATA, mapData)
                putExtra(EXTRA_MIN_VIEWS, minViews)
                putExtra(EXTRA_MAX_VIEWS, maxViews)
            }
            context.startActivity(intent)
        }
    }
}

data class CountriesDetailItem(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?
) : Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountriesDetailScreen(
    countries: List<CountriesDetailItem>,
    mapData: String,
    minViews: Long,
    maxViews: Long,
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
                // Map
                CountryMap(
                    mapData = mapData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MAP_ASPECT_RATIO)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                MapLegend(minViews = minViews, maxViews = maxViews)
                Spacer(modifier = Modifier.height(16.dp))
            }


            itemsIndexed(countries) { index, country ->
                DetailCountryRow(
                    position = index + 1,
                    country = country,
                    maxViews = countries.firstOrNull()?.views ?: 1L,
                    showDivider = index < countries.lastIndex
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CountryMap(
    mapData: String,
    modifier: Modifier = Modifier
) {
    val colorLow = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toHexString()
    val colorHigh = MaterialTheme.colorScheme.primary.toHexString()
    val backgroundColor = MaterialTheme.colorScheme.surface.toHexString()
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant.toHexString()
    val viewsLabel = stringResource(R.string.stats_countries_views_header)

    val htmlPage = remember(mapData, colorLow, colorHigh, backgroundColor, emptyColor, viewsLabel) {
        buildMapHtml(mapData, viewsLabel, colorLow, colorHigh, emptyColor, backgroundColor)
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
            }
        },
        update = { webView ->
            val base64Html = Base64.encodeToString(htmlPage.toByteArray(), Base64.DEFAULT)
            webView.loadData(base64Html, "text/html; charset=UTF-8", "base64")
        }
    )
}

private fun buildMapHtml(
    mapData: String,
    viewsLabel: String,
    colorLow: String,
    colorHigh: String,
    emptyColor: String,
    backgroundColor: String
): String {
    return """
        <html>
        <head>
        <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
        <script type="text/javascript">
            google.charts.load('current', {'packages':['geochart']});
            google.charts.setOnLoadCallback(drawRegionsMap);
            function drawRegionsMap() {
                var data = google.visualization.arrayToDataTable([
                    ['Country', '$viewsLabel'],$mapData
                ]);
                var options = {
                    keepAspectRatio: true,
                    region: 'world',
                    colorAxis: { colors: ['#$colorLow', '#$colorHigh'] },
                    datalessRegionColor: '#$emptyColor',
                    backgroundColor: '#$backgroundColor',
                    legend: 'none',
                    enableRegionInteractivity: false
                };
                var chart = new google.visualization.GeoChart(
                    document.getElementById('regions_div')
                );
                chart.draw(data, options);
            }
        </script>
        </head>
        <body style="margin: 0px;">
        <div id="regions_div" style="width: 100%; height: 100%;"></div>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun MapLegend(
    minViews: Long,
    maxViews: Long
) {
    val colorLow = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val colorHigh = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatStatValue(minViews),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(colorLow, colorHigh)
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatStatValue(maxViews),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailCountryRow(
    position: Int,
    country: CountriesDetailItem,
    maxViews: Long,
    showDivider: Boolean
) {
    val percentage = if (maxViews > 0) country.views.toFloat() / maxViews.toFloat() else 0f
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Background bar representing the percentage
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage)
                    .height(56.dp)
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

                // Views count
                Text(
                    text = formatStatValue(country.views),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("%06X", argb and 0xFFFFFF)
}

@Preview(showBackground = true)
@Composable
private fun CountriesDetailScreenPreview() {
    AppThemeM3 {
        CountriesDetailScreen(
            countries = listOf(
                CountriesDetailItem("US", "United States", 3464, null),
                CountriesDetailItem("ES", "Spain", 556, null),
                CountriesDetailItem("GB", "United Kingdom", 522, null),
                CountriesDetailItem("CA", "Canada", 485, null),
                CountriesDetailItem("DE", "Germany", 412, null),
                CountriesDetailItem("FR", "France", 387, null),
                CountriesDetailItem("AU", "Australia", 298, null),
                CountriesDetailItem("BR", "Brazil", 245, null),
                CountriesDetailItem("IN", "India", 201, null),
                CountriesDetailItem("MX", "Mexico", 156, null)
            ),
            mapData = "['US',3464],['ES',556],['GB',522],['CA',485]",
            minViews = 156,
            maxViews = 3464,
            onBackPressed = {}
        )
    }
}
