package org.wordpress.android.ui.blaze

import org.wordpress.android.fluxc.model.PostModel

sealed class BlazeUiState {
    sealed class PromoteScreen : BlazeUiState() {
        data class PromoteWithBlaze(val postModel: PostModel) : PromoteScreen()
        object PromoteWithSite : PromoteScreen()
    }
    object PostSelectionScreen : BlazeUiState()
    data class AppearanceScreen(val postModel: PostModel) : BlazeUiState()
    object AudienceScreen : BlazeUiState()
    object PaymentGateway : BlazeUiState()
}

enum class BlazeFlowSource(val trackingName: String) {
    STATS("stats"),
    DASHBOARD("dashboard"),
    MENU_ITEM("menu_item"),
    POST_LIST("post_list"),
    POST_STATS("post_stats")
}
