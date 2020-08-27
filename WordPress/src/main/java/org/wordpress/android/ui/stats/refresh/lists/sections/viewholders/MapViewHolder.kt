package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.annotation.SuppressLint
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.util.getColorFromAttribute

class MapViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_web_view_item
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val webView: WebView = itemView.findViewById(R.id.web_view)
    @SuppressLint("SetJavaScriptEnabled")
    fun bind(item: MapItem) {
        coroutineScope.launch {
            delay(100)
            // See: https://developers.google.com/chart/interactive/docs/gallery/geochart
            // Loading the v42 of the Google Charts API, since the latest stable version has a problem with
            // the legend. https://github.com/wordpress-mobile/WordPress-Android/issues/4131
            // https://developers.google.com/chart/interactive/docs/release_notes#release-candidate-details
            val colorLow = Integer.toHexString(
                    ContextCompat.getColor(
                            itemView.context,
                            R.color.stats_map_activity_low
                    ) and 0xffffff
            )
            val colorHigh = Integer.toHexString(
                    ContextCompat.getColor(
                            itemView.context,
                            R.color.stats_map_activity_high
                    ) and 0xffffff
            )
            val backgroundColor = Integer.toHexString(
                    itemView.context.getColorFromAttribute(R.attr.colorSurface) and 0xffffff
            )
            val emptyColor = Integer.toHexString(
                    ContextCompat.getColor(
                            itemView.context,
                            R.color.stats_map_activity_empty
                    ) and 0xffffff
            )
            val htmlPage = ("<html>" +
                    "<head>" +
                    "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
                    "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>" +
                    "<script type=\"text/javascript\">" +
                    " google.charts.load('42', {'packages':['geochart']});" +
                    " google.charts.setOnLoadCallback(drawRegionsMap);" +
                    " function drawRegionsMap() {" +
                    " var data = google.visualization.arrayToDataTable(" +
                    " [" +
                    " ['Country', '${itemView.resources.getString(item.label)}'],${item.mapData}]);" +
                    " var options = {keepAspectRatio: true, region: 'world', " +
                    " colorAxis: { colors: [ '#$colorLow', '#$colorHigh' ] }," +
                    " datalessRegionColor: '#$emptyColor'," +
                    " backgroundColor: '#$backgroundColor'," +
                    " legend: 'none'," +
                    " enableRegionInteractivity: false};" +
                    " var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));" +
                    " chart.draw(data, options);" +
                    " }" +
                    "</script>" +
                    "</head>" +
                    "<body style=\"margin: 0px;\">" +
                    "<div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>" +
                    "</body>" +
                    "</html>")

            val width = itemView.width
            val height = width * 5 / 8

            val params = webView.layoutParams
            val wrapperParams = itemView.layoutParams as RecyclerView.LayoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = height
            wrapperParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            wrapperParams.height = height

            launch(Dispatchers.Main) {
                webView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

                webView.layoutParams = params
                itemView.layoutParams = wrapperParams

                webView.webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError
                    ) {
                        super.onReceivedError(view, request, error)
                        itemView.visibility = View.GONE
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        super.onReceivedSslError(view, handler, error)
                        itemView.visibility = View.GONE
                    }
                }
                webView.settings.javaScriptEnabled = true
                webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                webView.loadData(htmlPage, "text/html", "UTF-8")
            }
        }
    }
}
