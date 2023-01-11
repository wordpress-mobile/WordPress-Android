package org.wordpress.android.ui.mysite.items.categoryheader

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

class SiteCategoryItemBuilder
@Inject constructor(private val themeBrowserUtils: ThemeBrowserUtils, private val siteUtilsWrapper: SiteUtilsWrapper) {
    fun buildJetpackCategoryIfAvailable(site: SiteModel): MySiteCardAndItem? {
        val jetpackSettingsVisible = site.isJetpackConnected && // jetpack is installed and connected
                !site.isWPComAtomic // isn't atomic site
        return if (jetpackSettingsVisible) {
            CategoryHeaderItem(UiStringRes(R.string.my_site_header_jetpack))
        } else null
    }

    fun buildLookAndFeelHeaderIfAvailable(site: SiteModel): MySiteCardAndItem? {
        return if (themeBrowserUtils.isAccessible(site)) {
            CategoryHeaderItem(UiStringRes(R.string.my_site_header_look_and_feel))
        } else null
    }

    fun buildConfigurationHeaderIfAvailable(site: SiteModel): MySiteCardAndItem? {
        // if either people or settings is visible, configuration header should be visible
        return if (site.hasCapabilityManageOptions ||
            !siteUtilsWrapper.isAccessedViaWPComRest(site) ||
            site.hasCapabilityListUsers
        ) {
            CategoryHeaderItem(UiStringRes(R.string.my_site_header_configuration))
        } else null
    }
}
