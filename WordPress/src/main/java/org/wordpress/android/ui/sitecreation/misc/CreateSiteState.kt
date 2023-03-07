package org.wordpress.android.ui.sitecreation.misc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CreateSiteState : Parcelable {
    /**
     * CreateSite request haven't finished yet or failed.
     */
    @Parcelize
    object SiteNotCreated : CreateSiteState()

    /**
     * FetchSite request haven't finished yet or failed.
     * Since we fetch the site without user awareness in background, the user may potentially leave the screen
     * before the request is finished.
     */
    @Parcelize
    data class SiteNotInLocalDb(val remoteSiteId: Long, val isSiteTitleTaskComplete: Boolean) : CreateSiteState()

    /**
     * The site has been successfully created and stored into local db.
     */
    @Parcelize
    data class SiteCreationCompleted(
        val localSiteId: Int,
        val isSiteTitleTaskComplete: Boolean,
        val url: String,
    ) : CreateSiteState()
}
