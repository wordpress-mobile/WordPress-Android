package org.wordpress.android.ui.pages

data class SnackbarMessageHolder(
    val message: String,
    val buttonTitle: String? = null,
    val buttonAction: () -> Unit = {}
)
