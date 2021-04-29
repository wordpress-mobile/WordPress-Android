package org.wordpress.android.ui.engagement

sealed class BottomSheetUiState {
    data class UserProfileUiState(
        val userAvatarUrl: String,
        val blavatarUrl: String,
        val userName: String,
        val userLogin: String,
        val userBio: String,
        val siteTitle: String,
        val siteUrl: String,
        val siteId: Long,
        val onSiteClickListener: ((siteId: Long, siteUrl: String, source: String) -> Unit)? = null,
        val blogPreviewSource: String
    ) : BottomSheetUiState() {
        val hasSiteUrl: Boolean = siteUrl.isNotBlank()
    }
}
