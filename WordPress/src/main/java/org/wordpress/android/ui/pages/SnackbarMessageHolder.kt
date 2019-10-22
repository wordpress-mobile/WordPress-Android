package org.wordpress.android.ui.pages

import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

data class SnackbarMessageHolder(
    @StringRes val messageRes: Int,
    @StringRes val buttonTitleRes: Int? = null,
    val buttonAction: () -> Unit = {},
    val onDismissAction: () -> Unit = {},
    val duration: Int = Snackbar.LENGTH_LONG
)
