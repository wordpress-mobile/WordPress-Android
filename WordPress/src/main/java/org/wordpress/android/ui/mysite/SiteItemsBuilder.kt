package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

class SiteItemsBuilder
@Inject constructor(private val siteUtilsWrapper: SiteUtilsWrapper, private val themeBrowserUtils: ThemeBrowserUtils) {
    fun buildSiteItems(site: SiteModel): List<MySiteItem> {
        val siteItems = mutableListOf<MySiteItem>()
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_publish)))
        if (themeBrowserUtils.isAccessible(site)) {
            siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_look_and_feel)))
        }
        // if either people or settings is visible, configuration header should be visible
        if (site.hasCapabilityManageOptions ||
                !siteUtilsWrapper.isAccessedViaWPComRest(site) ||
                site.hasCapabilityListUsers) {
            siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_configuration)))
        }
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_external)))
        return siteItems
    }
}
