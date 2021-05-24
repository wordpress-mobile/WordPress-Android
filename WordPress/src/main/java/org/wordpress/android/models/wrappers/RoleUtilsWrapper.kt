package org.wordpress.android.models.wrappers

import android.content.Context
import dagger.Reusable
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.RoleUtils
import javax.inject.Inject

@Reusable
class RoleUtilsWrapper @Inject constructor() {
    fun getInviteRoles(
        siteStore: SiteStore,
        siteModel: SiteModel,
        context: Context
    ): List<RoleModel> = RoleUtils.getInviteRoles(siteStore, siteModel, context)
}
