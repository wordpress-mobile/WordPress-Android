package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject

@Reusable
class PhotonUtilsWrapper @Inject constructor() {
    /*
     * returns true if the passed url is an obvious "mshots" url
     */
    fun isMshotsUrl(imageUrl: String?) = PhotonUtils.isMshotsUrl(imageUrl)
}
