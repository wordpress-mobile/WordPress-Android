package org.wordpress.android.ui.blaze

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class BlazeUiState {
    sealed class PromoteScreen : BlazeUiState() {
        data class PromotePost(val postUIModel: PostUIModel) : PromoteScreen()
        object Site : PromoteScreen()
    }

    object PostSelectionScreen : BlazeUiState()
    object AppearanceScreen : BlazeUiState()
    object AudienceScreen : BlazeUiState()
    object PaymentGateway : BlazeUiState()
    object Done : BlazeUiState()
}

enum class BlazeFlowSource(val trackingName: String) {
    STATS("stats"),
    DASHBOARD("dashboard"),
    MENU_ITEM("menu_item"),
    POST_LIST("post_list"),
    POST_STATS("post_stats")
}

@Parcelize
data class PostUIModel(
    val postId: Long,
    val title: String,
    val url: String,
    val imageUrl: Long,
) : Parcelable

