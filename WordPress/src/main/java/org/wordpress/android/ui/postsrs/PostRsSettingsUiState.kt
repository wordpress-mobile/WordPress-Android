package org.wordpress.android.ui.postsrs

import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus

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
    val publishDate: String = "",
    val password: String? = null,
    val authorName: FieldState = FieldState.Empty,
    val categoryNames: FieldState = FieldState.Empty,
    val tagNames: FieldState = FieldState.Empty,
    val featuredImage: FieldState = FieldState.Empty,
    val sticky: Boolean = false,
    val slug: String = "",
    val excerpt: String = "",
    val postStatus: PostStatus? = null,
    val postFormat: PostFormat? = null,
    val editedStatus: PostStatus? = null,
    val editedPassword: String? = null,
    val editedSticky: Boolean? = null,
    val editedSlug: String? = null,
    val editedExcerpt: String? = null,
    val editedFormat: PostFormat? = null,
    val isSaving: Boolean = false,
    val dialogState: DialogState = DialogState.None,
) {
    val hasChanges: Boolean
        get() = editedStatus != null ||
            editedPassword != null ||
            editedSticky != null ||
            editedSlug != null ||
            editedExcerpt != null ||
            editedFormat != null

    val effectivePassword: String?
        get() = editedPassword ?: password

    val effectiveSticky: Boolean
        get() = editedSticky ?: sticky

    val effectiveSlug: String
        get() = editedSlug ?: slug

    val effectiveExcerpt: String
        get() = editedExcerpt ?: excerpt
}

sealed interface DialogState {
    data object None : DialogState
    data object StatusDialog : DialogState
    data object PasswordDialog : DialogState
    data object SlugDialog : DialogState
    data object ExcerptDialog : DialogState
    data object FormatDialog : DialogState
    data object DiscardDialog : DialogState
}

enum class RetryableField {
    AUTHOR,
    CATEGORIES,
    TAGS,
}

sealed interface PostRsSettingsEvent {
    data object Finish : PostRsSettingsEvent
    data object FinishWithChanges : PostRsSettingsEvent
    data class ShowSnackbar(val message: String) : PostRsSettingsEvent
}
