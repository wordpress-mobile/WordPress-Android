package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder
@Inject constructor(
    private val siteCategoryItemBuilder: SiteCategoryItemBuilder
) {
    fun buildSiteItems(site: SiteModel): List<MySiteItem> {
        val siteItems = mutableListOf<MySiteItem>()
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(site))
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_publish)))
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(site))
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(site))
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_external)))
        return siteItems
    }

    private fun MutableList<MySiteItem>.addItemIfNotNull(item: MySiteItem?) {
        if (item != null) {
            this.add(item)
        }
    }
}
