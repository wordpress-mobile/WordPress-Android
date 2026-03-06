package org.wordpress.android.ui.postsrs

sealed interface FieldState {
    data object Empty : FieldState
    data object Loading : FieldState
    data class Loaded(val value: String) : FieldState
    data class Error(val message: String) : FieldState
}

data class PostRsSettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val postTitle: String = "",
    val statusLabel: String = "",
    val publishDate: String = "",
    val password: String? = null,
    val authorName: FieldState = FieldState.Empty,
    val categoryNames: FieldState = FieldState.Empty,
    val tagNames: FieldState = FieldState.Empty,
    val featuredImage: FieldState = FieldState.Empty,
    val sticky: Boolean = false,
    val formatLabel: String = "",
    val slug: String = "",
    val excerpt: String = "",
)

enum class RetryableField {
    AUTHOR,
    CATEGORIES,
    TAGS,
}

sealed interface PostRsSettingsEvent {
    data object Finish : PostRsSettingsEvent
    data class ShowSnackbar(val message: String) : PostRsSettingsEvent
}
