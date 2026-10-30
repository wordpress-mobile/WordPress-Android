package org.wordpress.android.ui.engagement

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class ListScenario @JvmOverloads constructor(
    val type: ListScenarioType,
    val source: EngagementNavigationSource,
    val siteId: Long,
    val postOrCommentId: Long,
    val commentPostId: Long = 0,
    val commentSiteUrl: String,
    val headerData: HeaderData,
    /**
     * Whether the list is headed by the item that was liked. Reader and notifications arrive with
     * the item's author and excerpt in hand and show it; New Stats knows only the item's id and
     * like count, and the screen it comes from already names the item, so it opts out.
     */
    val showLikedItemHeader: Boolean = true
) : Parcelable

enum class ListScenarioType(val typeDescription: String) {
    LOAD_POST_LIKES("post"),
    LOAD_COMMENT_LIKES("comment");

    companion object {
        fun getSourceDescription(type: ListScenarioType?): String {
            return type?.typeDescription ?: "unknown"
        }
    }
}

@Parcelize
@SuppressLint("ParcelCreator")
data class HeaderData constructor(
    val authorName: AuthorName,
    val snippetText: String,
    val authorAvatarUrl: String,
    val authorUserId: Long,
    val authorPreferredSiteId: Long,
    val authorPreferredSiteUrl: String,
    val numLikes: Int = 0
) : Parcelable

@SuppressLint("ParcelCreator")
sealed class AuthorName : Parcelable {
    @Parcelize
    data class AuthorNameString(val nameString: String?) : AuthorName()

    @Parcelize
    data class AuthorNameCharSequence(val nameCharSequence: CharSequence?) : AuthorName()
}
