package org.wordpress.android.ui.mysite

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder
@Inject constructor(
    private val siteCategoryItemBuilder: SiteCategoryItemBuilder,
    private val siteListItemBuilder: SiteListItemBuilder
) {
    fun buildSiteItems(site: SiteModel, onClick: ListItemInteraction): List<MySiteItem> {
        return listOfNotNull(
                siteListItemBuilder.buildPlanItemIfAvailable(site, onClick),
                siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(site),
                ListItem(
                        drawable.ic_stats_alt_white_24dp,
                        UiStringRes(string.stats),
                        onClick = onClick
                ),
                siteListItemBuilder.buildActivityLogItemIfAvailable(site, onClick),
                siteListItemBuilder.buildScanItemIfAvailable(onClick),
                siteListItemBuilder.buildJetpackItemIfAvailable(site, onClick),
                CategoryHeader(UiStringRes(string.my_site_header_publish)),
                siteListItemBuilder.buildPagesItemIfAvailable(site, onClick),
                ListItem(
                        drawable.ic_posts_white_24dp,
                        UiStringRes(string.my_site_btn_blog_posts),
                        onClick = onClick
                ),
                ListItem(
                        drawable.ic_media_white_24dp,
                        UiStringRes(string.media),
                        onClick = onClick
                ),
                ListItem(
                        drawable.ic_comment_white_24dp,
                        UiStringRes(string.my_site_btn_comments),
                        onClick = onClick
                ),
                siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(site),
                siteListItemBuilder.buildThemesItemIfAvailable(site, onClick),
                siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(site),
                siteListItemBuilder.buildPeopleItemIfAvailable(site, onClick),
                siteListItemBuilder.buildPluginItemIfAvailable(site, onClick),
                siteListItemBuilder.buildShareItemIfAvailable(site, onClick),
                siteListItemBuilder.buildSiteSettingsItemIfAvailable(site, onClick),
                CategoryHeader(UiStringRes(string.my_site_header_external)),
                ListItem(
                        drawable.ic_globe_white_24dp,
                        UiStringRes(string.my_site_btn_view_site),
                        secondaryIcon = drawable.ic_external_white_24dp,
                        onClick = onClick
                ),
                siteListItemBuilder.buildAdminItemIfAvailable(site, onClick),
        )
    }
}
