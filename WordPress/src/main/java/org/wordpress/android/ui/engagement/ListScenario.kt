package org.wordpress.android.ui.engagement

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ListScenario(
    val type: ListScenarioType,
    val siteId: Long,
    val itemId: Long,
    val headerData: HeaderData?
) : Parcelable

enum class ListScenarioType {
    LOAD_POST_LIKES,
    LOAD_COMMENT_LIKES
}

@Parcelize
data class HeaderData constructor(
    val name: UserName,
    val snippet: String,
    val avatarUrl: String,
    val userId: Long,
    val userSiteId: Long,
    val siteUrl: String
) : Parcelable

sealed class UserName : Parcelable {
    @Parcelize
    data class UserNameString(val nameString: String?) : UserName()
    @Parcelize
    data class UserNameCharSequence(val nameCharSequence: CharSequence?) : UserName()
}
