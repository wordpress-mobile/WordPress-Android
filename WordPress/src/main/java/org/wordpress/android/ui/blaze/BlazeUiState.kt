package org.wordpress.android.ui.blaze

sealed class BlazeUiState {
    sealed class PromoteScreen : BlazeUiState() {
        object PromotePost : PromoteScreen()
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

sealed class PostUIModel(
    val postId: Int,
    val title: String,
    val url: String,
    val imageUrl: String,
)

