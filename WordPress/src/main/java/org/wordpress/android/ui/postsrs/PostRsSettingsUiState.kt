package org.wordpress.android.ui.postsrs

import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus

data class PostRsSettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val postTitle: String = "",
    val status: PostStatus? = null,
    val statusLabel: String = "",
    val publishDate: String = "",
    val password: String? = null,
    val authorId: Long = 0L,
    val authorDisplayName: String? = null,
    val categoryIds: List<Long> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val featuredImageId: Long = 0L,
    val featuredImageUrl: String? = null,
    val sticky: Boolean = false,
    val format: PostFormat? = null,
    val formatLabel: String = "",
    val slug: String = "",
    val excerpt: String = "",
)

sealed interface PostRsSettingsEvent {
    data object Finish : PostRsSettingsEvent
    data class ShowSnackbar(val message: String) : PostRsSettingsEvent
}
