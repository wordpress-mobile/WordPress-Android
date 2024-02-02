package org.wordpress.android.ui.sitemonitor

import androidx.annotation.StringRes
import org.wordpress.android.R

const val METRICS_URL_TEMPLATE = "https://wordpress.com/site-monitoring/{blog}"
const val PHPLOGS_URL_TEMPLATE = "https://wordpress.com/site-monitoring/{blog}/php"
const val WEBSERVERLOGS_URL_TEMPLATE = "https://wordpress.com/site-monitoring/{blog}/web"

enum class SiteMonitorTabItem(
    val route: String,
    @StringRes val title: Int,
    val urlTemplate: String,
    val siteMonitorType: SiteMonitorType
) {
    Metrics(
        "metrics",
        R.string.site_monitoring_tab_title_metrics,
        METRICS_URL_TEMPLATE,
        SiteMonitorType.METRICS
    ),
    PHPLogs(
        "phplogs",
        R.string.site_monitoring_tab_title_php_logs,
        PHPLOGS_URL_TEMPLATE,
        SiteMonitorType.PHP_LOGS
    ),
    WebServerLogs(
        "webserverlogs",
        R.string.site_monitoring_tab_title_web_server_logs,
        WEBSERVERLOGS_URL_TEMPLATE,
        SiteMonitorType.WEB_SERVER_LOGS
    );
}
