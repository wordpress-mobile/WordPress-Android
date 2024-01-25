package org.wordpress.android.ui.sitemonitor

import javax.inject.Inject

class SiteMonitorMapper @Inject constructor(
    private val siteMonitorUtils: SiteMonitorUtils
) {
    fun toPrepared(urls: List<SiteMonitorUrl>) = SiteMonitorUiState.Prepared(
        model = SiteMonitorModel(
            enableJavascript = true,
            enableDomStorage = true,
            userAgent = siteMonitorUtils.getUserAgent(),
            enableChromeClient = true,
            urls = urls
        )
    )

    fun toNoNetworkError(buttonClick: () -> Unit) = SiteMonitorUiState.NoNetworkError(buttonClick = buttonClick)

    fun toGenericError(buttonClick: () -> Unit) = SiteMonitorUiState.GenericError(buttonClick =  buttonClick)
}
