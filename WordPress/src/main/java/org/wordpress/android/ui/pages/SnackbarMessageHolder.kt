package org.wordpress.android.ui.pages

import org.wordpress.android.ui.utils.UiString

const val INVALID_MESSAGE_RES = -1

data class SnackbarMessageHolder(
    val message: UiString,
    val buttonTitle: UiString? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: () -> Unit = {},
    val message: CharSequence? = null
)
