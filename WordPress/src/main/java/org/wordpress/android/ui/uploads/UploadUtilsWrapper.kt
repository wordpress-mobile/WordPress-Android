package org.wordpress.android.ui.uploads

import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

/**
 * Injectable wrapper around UploadUtils.
 *
 * UploadUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class UploadUtilsWrapper @Inject constructor() {
    fun userCanPublish(site: SiteModel): Boolean {
        return UploadUtils.userCanPublish(site)
    }
}
