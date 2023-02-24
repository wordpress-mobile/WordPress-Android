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
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item"),
    POSTS_LIST("posts_list"),
    STATS_POST("stats_post")
}

@Parcelize
data class PostUIModel(
    val postId: Long,
    val title: String,
    val url: String,
    val imageUrl: Long,
    val featuredImageUrl: String?
) : Parcelable

