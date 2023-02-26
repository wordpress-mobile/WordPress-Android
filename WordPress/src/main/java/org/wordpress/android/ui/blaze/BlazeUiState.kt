package org.wordpress.android.ui.blaze

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.WordPress

sealed class BlazeUiState {
    sealed class PromoteScreen : BlazeUiState() {
        data class PromotePost(val postUIModel: PostUIModel) : PromoteScreen()
        object Site : PromoteScreen()
        object Page : PromoteScreen()
    }
    object WebViewScreen : BlazeUiState()
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
) : Parcelable

sealed class BlazeWebViewHeaderUiState {
    @StringRes val headerTitle = R.string.blaze_activity_title
    @StringRes val headerActionText = R.string.cancel
    open val headerActionVisible: Boolean = true

    data class ShowAction(
        override val headerActionVisible: Boolean = true
    ): BlazeWebViewHeaderUiState()

    data class HideAction(
        override val headerActionVisible: Boolean = false
    ): BlazeWebViewHeaderUiState()
}

data class BlazeWebViewContentUiState(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val userAgent: String = WordPress.getUserAgent(),
    val enableChromeClient: Boolean = true,
    val addressToLoad: String = "",
    val url: String = ""
)
