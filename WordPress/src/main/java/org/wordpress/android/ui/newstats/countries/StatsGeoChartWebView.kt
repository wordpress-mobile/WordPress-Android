package org.wordpress.android.ui.newstats.countries

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.http.SslError
import android.util.Base64
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF

/**
 * A WebView component for displaying Google GeoChart maps.
 *
 * Security measures implemented (following the pattern from MapViewHolder in old stats):
 * - Custom WebViewClient with error handlers for graceful degradation
 * - Handles SSL errors by hiding the view (does not proceed with insecure connections)
 * - Handles resource errors gracefully
 * - Loads HTML content as base64 data (not from external URLs)
 * - JavaScript is enabled only for Google Charts functionality
 *
 * @param mapData The map data string in Google GeoChart format
 * @param modifier Modifier for the WebView container
 * @param onError Optional callback when an error occurs loading the map
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StatsGeoChartWebView(
    mapData: String,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    // Use the same colors as old stats implementation (MapViewHolder pattern)
    val colorLow = ContextCompat.getColor(context, R.color.stats_map_activity_low).toHexString()
    val colorHigh = ContextCompat.getColor(context, R.color.stats_map_activity_high).toHexString()
    val emptyColor = ContextCompat.getColor(context, R.color.stats_map_activity_empty).toHexString()
    val backgroundColor = MaterialTheme.colorScheme.surface.toHexString()
    val viewsLabel = stringResource(R.string.stats_countries_views_header)

    val htmlPage = remember(mapData, colorLow, colorHigh, backgroundColor, emptyColor, viewsLabel) {
        buildGeoChartHtml(mapData, viewsLabel, colorLow, colorHigh, emptyColor, backgroundColor)
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)

                // Set up WebViewClient with error handlers (matching old stats MapViewHolder pattern)
                webViewClient = createWebViewClientWithErrorHandlers(onError)

                // Settings matching the old stats implementation
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

/**
 * Creates a WebViewClient with error handlers for graceful degradation.
 * This follows the same pattern as MapViewHolder in the old stats implementation.
 */
private fun createWebViewClientWithErrorHandlers(onError: (() -> Unit)?): WebViewClient {
    return object : WebViewClient() {
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            // Trigger error callback for main frame errors
            if (request?.isForMainFrame == true) {
                onError?.invoke()
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            // Do not proceed on SSL errors - this is the secure default behavior
            super.onReceivedSslError(view, handler, error)
            onError?.invoke()
        }
    }
}

@Suppress("LongParameterList")
private fun buildGeoChartHtml(
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

private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    return String.format(Locale.US, "%06X", (this.toArgb() and RGB_MASK))
}

private fun Int.toHexString(): String {
    return String.format(Locale.US, "%06X", (this and RGB_MASK))
}
