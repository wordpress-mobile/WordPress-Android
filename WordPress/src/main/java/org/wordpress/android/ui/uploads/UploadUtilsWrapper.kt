package org.wordpress.android.ui.uploads

import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around UploadUtils.
 *
 * UploadUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Singleton
class UploadUtilsWrapper @Inject constructor() {
    fun userCanPublish(site: SiteModel): Boolean {
        return UploadUtils.userCanPublish(site)
    }
}
