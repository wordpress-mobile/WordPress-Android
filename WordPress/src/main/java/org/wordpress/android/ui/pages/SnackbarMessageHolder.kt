package org.wordpress.android.ui.pages

import android.support.annotation.StringRes

data class SnackbarMessageHolder(
    @StringRes val messageRes: Int,
    @StringRes val buttonTitleRes: Int? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: () -> Unit = {}
)
