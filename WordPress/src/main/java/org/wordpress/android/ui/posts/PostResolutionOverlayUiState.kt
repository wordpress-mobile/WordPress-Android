package org.wordpress.android.ui.posts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.utils.UiString

data class PostResolutionOverlayUiState(
    @StringRes val titleResId: Int,
    @StringRes val bodyResId: Int,
    val actionEnabled: Boolean = false,
    val content: List<ContentItem>,
    val selectedContentItem: ContentItem? = null,
    val onSelected: (ContentItem) -> Unit,
    val closeClick: () -> Unit,
    val cancelClick: () -> Unit,
    val confirmClick: () -> Unit
)

data class ContentItem(
    val id: ContentItemType,
    @DrawableRes val iconResId: Int = R.drawable.ic_pages_white_24dp,
    @StringRes val headerResId: Int,
    val dateLine: UiString,
    val isSelected: Boolean,
)

enum class ContentItemType {
    LOCAL_DEVICE,
    OTHER_DEVICE
}

fun ContentItemType.toPostResolutionConfirmationType(): PostResolutionConfirmationType {
    return when (this) {
        ContentItemType.LOCAL_DEVICE -> PostResolutionConfirmationType.CONFIRM_LOCAL
        ContentItemType.OTHER_DEVICE -> PostResolutionConfirmationType.CONFIRM_OTHER
    }
}

enum class PostResolutionType {
    SYNC_CONFLICT,
    AUTOSAVE_REVISION_CONFLICT
}

enum class PostResolutionConfirmationType(val analyticsLabel: String) {
    CONFIRM_LOCAL("local_version"),
    CONFIRM_OTHER("remote_version")
}

sealed class PostResolutionOverlayActionEvent {
    data class ShowDialogAction(val postModel: PostModel, val postResolutionType: PostResolutionType)
    data class PostResolutionConfirmationEvent(
        val postResolutionType: PostResolutionType,
        val postResolutionConfirmationType: PostResolutionConfirmationType
    )
}
