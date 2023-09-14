package org.wordpress.android.ui.blaze

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R

sealed class BlazeUiState {
    sealed class PromoteScreen : BlazeUiState() {
        data class PromotePost(val postUIModel: PostUIModel) : PromoteScreen()
        object Site : PromoteScreen()
        data class PromotePage(val pagesUIModel: PageUIModel) : PromoteScreen()
    }
    object WebViewScreen : BlazeUiState()
    object Done : BlazeUiState()
}

enum class BlazeFlowSource(val trackingName: String) {
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item"),
    POSTS_LIST("posts_list"),
    STATS_POST("stats_post"),
    PAGES_LIST("pages_list"),
    CAMPAIGN_LISTING_PAGE("campaign_listing_page")
}
sealed interface BlazeUIModel: Parcelable {
    val title: String
    val url: String
    val featuredImageUrl: String?
}

@Parcelize
data class PostUIModel(
    val postId: Long,
    override val title: String,
    override val url: String,
    val featuredImageId: Long,
    override val featuredImageUrl: String?
) : BlazeUIModel

@Parcelize
data class PageUIModel(
    val pageId: Long,
    override val title: String,
    override val url: String,
    val featuredImageId: Long,
    override val featuredImageUrl: String?
) : BlazeUIModel

sealed class BlazeWebViewHeaderUiState(
    @StringRes open val headerActionText: Int,
    open val headerActionEnabled: Boolean = true
) {
    @StringRes val headerTitle = R.string.blaze_activity_title

    data class EnabledCancelAction(
        @StringRes override val headerActionText: Int = R.string.cancel,
        override val headerActionEnabled: Boolean = true
    ): BlazeWebViewHeaderUiState(headerActionText, headerActionEnabled)

    data class DisabledCancelAction(
        @StringRes override val headerActionText: Int = R.string.cancel,
        override val headerActionEnabled: Boolean = false
    ): BlazeWebViewHeaderUiState(headerActionText, headerActionEnabled)

    data class DoneAction(
        @StringRes override val headerActionText: Int = R.string.blaze_header_done_label,
        override val headerActionEnabled: Boolean = true
    ): BlazeWebViewHeaderUiState(headerActionText, headerActionEnabled)
}

enum class BlazeFlowStep(val label: String, val trackingName: String) {
    CAMPAIGNS_LIST("campaigns_list", "campaigns_list"),
    POSTS_LIST("posts_list", "posts_list"),
    STEP_1("step-1", "step_1"),
    STEP_2("step-2","step_2"),
    STEP_3("step-3","step_3"),
    STEP_4("step-4","step_4"),
    STEP_5("step-5","step_5"),
    UNSPECIFIED("unspecified", "unspecified");

    override fun toString() = label

    companion object {
        @JvmStatic
        fun fromString(strSource: String?): BlazeFlowStep =
            strSource?.let { source ->
                values().firstOrNull { it.label.equals(source, ignoreCase = true) }
            } ?: UNSPECIFIED
    }
}
