package org.wordpress.android.ui.reader.subscription

data class ReaderSubscriptionSettingsUiState(
    val blogId: Long,
    val blogName: String,
    val blogUrl: String,
    val isLoading: Boolean = false,
    val notifyPostsEnabled: Boolean = false,
    val emailPostsEnabled: Boolean = false,
    val emailCommentsEnabled: Boolean = false
)
