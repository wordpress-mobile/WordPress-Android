package org.wordpress.android.ui.newstats.countries

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF
private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private const val MAP_ASPECT_RATIO = 8f / 5f

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
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 500f, 0f),
        end = Offset(translateAnimation.value, 0f)
    )

    Column(modifier = Modifier.padding(CardPadding)) {
        // Title placeholder
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Map placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MAP_ASPECT_RATIO)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Legend placeholder
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List items placeholders
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
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
            MapLegend(
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
            val maxViews = state.countries.maxOfOrNull { it.views } ?: 1L
            state.countries.forEachIndexed { index, country ->
                val percentage = if (maxViews > 0) country.views.toFloat() / maxViews.toFloat() else 0f
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
            text = stringResource(R.string.stats_no_data_for_period),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CountryMap(
    mapData: String,
    modifier: Modifier = Modifier
) {
    val colorLow = MaterialTheme.colorScheme.primary.blendWithWhite(0.2f).toHexString()
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

@Suppress("LongParameterList")
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

            // Views count
            Text(
                text = formatStatValue(country.views),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format(Locale.US, "%06X", argb and RGB_MASK)
}

/**
 * Blends this color with white based on the given ratio.
 * Ratio of 0.0 returns white, ratio of 1.0 returns the original color.
 */
private fun androidx.compose.ui.graphics.Color.blendWithWhite(ratio: Float): androidx.compose.ui.graphics.Color {
    val white = androidx.compose.ui.graphics.Color.White
    return androidx.compose.ui.graphics.Color(
        red = white.red + (this.red - white.red) * ratio,
        green = white.green + (this.green - white.green) * ratio,
        blue = white.blue + (this.blue - white.blue) * ratio,
        alpha = 1f
    )
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
                    CountryItem("US", "United States", 3464, null),
                    CountryItem("ES", "Spain", 556, null),
                    CountryItem("GB", "United Kingdom", 522, null),
                    CountryItem("CA", "Canada", 485, null)
                ),
                mapData = "['US',3464],['ES',556],['GB',522],['CA',485]",
                minViews = 485,
                maxViews = 3464,
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
