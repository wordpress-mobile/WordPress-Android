package org.wordpress.android.ui.postsrs

data class PostRsSettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val postTitle: String = "",
    val statusLabel: String = "",
    val publishDate: String = "",
    val password: String? = null,
    val authorDisplayName: String? = null,
    val categoryIds: List<Long> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val featuredImageId: Long = 0L,
    val featuredImageUrl: String? = null,
    val sticky: Boolean = false,
    val formatLabel: String = "",
    val slug: String = "",
    val excerpt: String = "",
)

sealed interface PostRsSettingsEvent {
    data object Finish : PostRsSettingsEvent
    data class ShowSnackbar(val message: String) : PostRsSettingsEvent
}
