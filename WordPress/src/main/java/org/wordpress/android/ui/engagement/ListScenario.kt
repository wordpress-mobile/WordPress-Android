package org.wordpress.android.ui.engagement

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ListScenario(
    val type: ListScenarioType,
    val siteId: Long,
    val postOrCommentId: Long,
    val commentPostId: Long = 0,
    val commentSiteUrl: String,
    val headerData: HeaderData?
) : Parcelable

enum class ListScenarioType {
    LOAD_POST_LIKES,
    LOAD_COMMENT_LIKES
}

@Parcelize
data class HeaderData constructor(
    val authorName: AuthorName,
    val snippetText: String,
    val authorAvatarUrl: String,
    val authorUserId: Long,
    val authorPreferredSiteId: Long,
    val authorPreferredSiteUrl: String
) : Parcelable

sealed class AuthorName : Parcelable {
    @Parcelize
    data class AuthorNameString(val nameString: String?) : AuthorName()
    @Parcelize
    data class AuthorNameCharSequence(val nameCharSequence: CharSequence?) : AuthorName()
}
