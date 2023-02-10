package org.wordpress.android.ui.blaze

import org.wordpress.android.fluxc.model.PostModel

sealed class UIStatus {
    sealed class PromoteScreen : UIStatus() {
        data class PromoteWithBlaze(val postModel: PostModel) : PromoteScreen()
        object PromoteWithSite : PromoteScreen()
    }
    object PostSelectionScreen : UIStatus()
    data class AppearanceScreen(val postModel: PostModel) : UIStatus()
    object AudienceScreen : UIStatus()
    object PaymentGateway : UIStatus()
}

enum class BlazeFlowSource(val trackingName: String) {
    STATS("stats"),
    DASHBOARD("dashboard"),
    MENU_ITEM("menu_item"),
    POST_LIST("post_list"),
    POST_STATS("post_stats")
}
