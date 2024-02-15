package org.wordpress.android.ui.sitemonitor

import javax.inject.Inject

class SiteMonitorMapper @Inject constructor(
    private val siteMonitorUtils: SiteMonitorUtils
) {
    fun toPrepared(url: String, addressToLoad: String, siteMonitorType: SiteMonitorType) = SiteMonitorUiState.Prepared(
        model = SiteMonitorModel(
            enableJavascript = true,
            enableDomStorage = true,
            userAgent = siteMonitorUtils.getUserAgent(),
            enableChromeClient = true,
            url = url,
            addressToLoad = addressToLoad,
            siteMonitorType = siteMonitorType
        )
    )

    fun toNoNetworkError(buttonClick: () -> Unit) = SiteMonitorUiState.NoNetworkError(buttonClick = buttonClick)

    fun toGenericError(buttonClick: () -> Unit) = SiteMonitorUiState.GenericError(buttonClick =  buttonClick)
}
