package org.wordpress.android.ui.pages

import androidx.annotation.StringRes
import org.wordpress.android.ui.utils.UiString

data class SnackbarMessageHolder(
    val message: UiString,
    @StringRes val buttonTitleRes: Int? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: () -> Unit = {}
)
