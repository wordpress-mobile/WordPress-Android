package org.wordpress.android.ui.main

import androidx.annotation.StringRes

data class MainFabUiState(
    val isFabVisible: Boolean,
    @StringRes val CreateContentMessageId: Int,
    val isFocusPointVisible: Boolean = false
)
