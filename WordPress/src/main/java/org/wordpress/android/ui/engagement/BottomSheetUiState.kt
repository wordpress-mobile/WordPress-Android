package org.wordpress.android.ui.engagement

import java.io.Serializable

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
        val blogPreviewSource: String
    ) : BottomSheetUiState(), Serializable {
        val hasSiteUrl: Boolean = siteUrl.isNotBlank()
    }
}
