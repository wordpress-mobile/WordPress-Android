package org.wordpress.android.ui.pages

import org.wordpress.android.ui.utils.UiString

data class SnackbarMessageHolder(
    val message: UiString,
    val buttonTitle: UiString? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: (event: Int) -> Unit = {}
)
