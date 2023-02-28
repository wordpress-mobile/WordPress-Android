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
    val featuredImageUrl: String?
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

enum class BlazeFlowStep(val trackingName: String) {
    CAMPAIGNS_LIST("campaigns_list"),
    POSTS_LIST("posts_list"),
    STEP_1("step_1"),
    STEP_2("step_2"),
    STEP_3("step_3"),
    STEP_4("step_4"),
    STEP_5("step_5"),
    UNSPECIFIED("unspecified");

    override fun toString() = trackingName

    companion object {
        @JvmStatic
        fun fromString(strSource: String?): BlazeFlowStep =
            strSource?.let { source ->
                values().firstOrNull { it.name.equals(source, ignoreCase = true) }
            } ?: UNSPECIFIED
    }
}
