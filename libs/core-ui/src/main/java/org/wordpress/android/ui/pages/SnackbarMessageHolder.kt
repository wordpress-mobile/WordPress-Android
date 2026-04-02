package org.wordpress.android.ui.pages

import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.ui.utils.UiString

data class SnackbarMessageHolder(
    val message: UiString,
    val buttonTitle: UiString? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: (event: Int) -> Unit = {},
    val duration: Int = Snackbar.LENGTH_LONG,
    val isImportant: Boolean = true
)
