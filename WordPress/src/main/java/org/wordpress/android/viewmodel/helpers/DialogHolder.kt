package org.wordpress.android.viewmodel.helpers

import android.support.annotation.StringRes

class DialogHolder(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val positiveButtonTextRes: Int,
    @StringRes val negativeButtonTextRes: Int,
    val positiveButtonAction: () -> Unit,
    val cancelable: Boolean = true
)
