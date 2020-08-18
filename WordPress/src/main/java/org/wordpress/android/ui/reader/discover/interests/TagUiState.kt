package org.wordpress.android.ui.reader.discover.interests

data class TagUiState(
    val title: String,
    val slug: String,
    val isChecked: Boolean = false,
    val onClick: ((String) -> Unit)? = null,
    val maxWidth: Int = Int.MAX_VALUE
)
