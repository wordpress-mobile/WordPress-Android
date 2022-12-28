package org.wordpress.android.ui.engagement

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class ListScenario(
    val type: ListScenarioType,
    val source: EngagementNavigationSource,
    val siteId: Long,
    val postOrCommentId: Long,
    val commentPostId: Long = 0,
    val commentSiteUrl: String,
    val headerData: HeaderData
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
