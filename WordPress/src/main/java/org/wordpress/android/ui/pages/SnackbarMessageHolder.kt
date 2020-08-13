package org.wordpress.android.ui.pages

import androidx.annotation.StringRes

const val INVALID_MESSAGE_RES = -1

data class SnackbarMessageHolder(
    @StringRes val messageRes: Int,
    @StringRes val buttonTitleRes: Int? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: () -> Unit = {},
    val message: CharSequence? = null
)
