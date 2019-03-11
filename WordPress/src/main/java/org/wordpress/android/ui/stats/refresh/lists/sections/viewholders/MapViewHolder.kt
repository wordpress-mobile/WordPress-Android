package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.net.http.SslError
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout.LayoutParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem

class MapViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_web_view_item
) {
    val webView = itemView.findViewById<WebView>(id.web_view)
    fun bind(item: MapItem) {
        GlobalScope.launch {
            delay(100)
            // See: https://developers.google.com/chart/interactive/docs/gallery/geochart
            // Loading the v42 of the Google Charts API, since the latest stable version has a problem with
            // the legend. https://github.com/wordpress-mobile/WordPress-Android/issues/4131
            // https://developers.google.com/chart/interactive/docs/release_notes#release-candidate-details
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
                    " var options = {keepAspectRatio: true, region: 'world', colorAxis:" +
                    " { colors: [ '#FFF088', '#F24606' ] }, enableRegionInteractivity: false};" +
                    " var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));" +
                    " chart.draw(data, options);" +
                    " }" +
                    "</script>" +
                    "</head>" +
                    "<body>" +
                    "<div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>" +
                    "</body>" +
                    "</html>")

            val width = itemView.width
            val height = width * 3 / 4

            val params = webView.layoutParams as LayoutParams
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

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
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
