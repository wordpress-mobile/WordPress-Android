package org.wordpress.android.ui.postsrs

import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import java.util.Date
import java.util.TimeZone

internal val UTC: TimeZone = TimeZone.getTimeZone("UTC")

data class AuthorInfo(val id: Long, val name: String)

sealed interface FieldState {
    data object Empty : FieldState
    data object Loading : FieldState
    data class Loaded(val value: String) : FieldState
    data class Error(val message: String) : FieldState
}

data class PostRsSettingsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val postTitle: String = "",
    val publishDate: String = "",
    val originalDate: Date? = null,
    val authorId: Long = 0L,
    val siteAuthors: List<AuthorInfo> = emptyList(),
    val isLoadingMoreAuthors: Boolean = false,
    val canLoadMoreAuthors: Boolean = false,
    val authorSearchQuery: String = "",
    val isSearchingAuthors: Boolean = false,
    val canEditAuthor: Boolean = false,
    val password: String? = null,
    val authorName: FieldState = FieldState.Empty,
    val categoryNames: FieldState = FieldState.Empty,
    val tagNames: FieldState = FieldState.Empty,
    val categoryIds: List<Long> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val featuredImage: FieldState = FieldState.Empty,
    val featuredImageId: Long = 0L,
    val editedCategoryIds: List<Long>? = null,
    val editedTagIds: List<Long>? = null,
    val editedFeaturedImageId: Long? = null,
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
    val editedDate: Date? = null,
    val editedAuthor: Long? = null,
    val isSaving: Boolean = false,
    val dialogState: DialogState = DialogState.None,
) {
    val hasChanges: Boolean
        get() = editedStatus != null ||
            editedPassword != null ||
            editedSticky != null ||
            editedSlug != null ||
            editedExcerpt != null ||
            editedFormat != null ||
            editedDate != null ||
            editedAuthor != null ||
            editedFeaturedImageId != null ||
            editedCategoryIds != null ||
            editedTagIds != null

    val effectivePassword: String?
        get() = editedPassword ?: password

    val effectiveSticky: Boolean
        get() = editedSticky ?: sticky

    val effectiveSlug: String
        get() = editedSlug ?: slug

    val effectiveExcerpt: String
        get() = editedExcerpt ?: excerpt

    val effectiveDate: Date?
        get() = editedDate ?: originalDate

    val effectiveAuthorId: Long
        get() = editedAuthor ?: authorId

    val effectiveFeaturedImageId: Long
        get() = editedFeaturedImageId ?: featuredImageId

    val effectiveCategoryIds: List<Long>
        get() = editedCategoryIds ?: categoryIds

    val effectiveTagIds: List<Long>
        get() = editedTagIds ?: tagIds
}

sealed interface DialogState {
    data object None : DialogState
    data object StatusDialog : DialogState
    data object PasswordDialog : DialogState
    data object SlugDialog : DialogState
    data object ExcerptDialog : DialogState
    data object FormatDialog : DialogState
    data object DateDialog : DialogState
    data object TimeDialog : DialogState
    data object AuthorDialog : DialogState
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
    data object LaunchWpMediaPicker : PostRsSettingsEvent
    data object LaunchDeviceMediaPicker : PostRsSettingsEvent
    data class LaunchCategorySelection(
        val selectedIds: List<Long>,
    ) : PostRsSettingsEvent
    data class LaunchTagSelection(
        val selectedIds: List<Long>,
    ) : PostRsSettingsEvent
}
