package org.wordpress.android.ui.blaze.blazepromote

import org.wordpress.android.R
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.utils.UiString
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

    fun toNoNetworkError(buttonClick: () -> Unit) = BlazePromoteUiState.Error(
        title = UiString.UiStringRes(R.string.blaze_promote_no_network_error_title),
        description = UiString.UiStringRes(R.string.blaze_promote_error_description),
        button = BlazePromoteUiState.Error.ErrorButton(
            text = UiString.UiStringRes(R.string.blaze_promote_error_button_text),
            click = buttonClick
        )
    )

    fun toGenericError(buttonClick: () -> Unit) = BlazePromoteUiState.Error(
        title = UiString.UiStringRes(R.string.blaze_promote_error_title),
        description = UiString.UiStringRes(R.string.blaze_promote_error_description),
        button = BlazePromoteUiState.Error.ErrorButton(
            text = UiString.UiStringRes(R.string.blaze_promote_error_button_text),
            click = buttonClick
        )
    )
}

