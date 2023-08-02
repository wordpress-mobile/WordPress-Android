package org.wordpress.android.ui.blaze.blazepromote

import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

class BlazePromoteMapper @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
) {
    fun toLoading(url: String, addressToLoad: String) = BlazePromoteUiState.Loading(
        model = BlazePromoteModel(
            enableJavascript = true,
            enableDomStorage = true,
            userAgent = blazeFeatureUtils.getUserAgent(),
            enableChromeClient = true,
            url = url,
            addressToLoad = addressToLoad
        )
    )

    fun toNoNetworkError(buttonClick: () -> Unit) = BlazePromoteUiState.NoNetworkError(buttonClick = buttonClick)

    fun toGenericError(buttonClick: () -> Unit) = BlazePromoteUiState.GenericError(buttonClick = buttonClick)
}
