package org.wordpress.android.ui.mysite

import org.wordpress.android.R
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
    fun buildSiteItems(site: SiteModel, onClick: (ListItemAction) -> Unit): List<MySiteItem> {
        val siteItems = mutableListOf<MySiteItem>()
        siteItems.addItemIfNotNull(siteListItemBuilder.buildPlanItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(site))
        siteItems.add(
                ListItem(
                        R.drawable.ic_stats_alt_white_24dp,
                        UiStringRes(R.string.stats),
                        onClick = ListItemInteraction.create(ListItemAction.STATS, onClick)
                )
        )
        siteItems.addItemIfNotNull(siteListItemBuilder.buildActivityLogItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildScanItemIfAvailable(onClick))
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_publish)))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildPagesItemIfAvailable(site, onClick))
        siteItems.add(
                ListItem(
                        R.drawable.ic_posts_white_24dp,
                        UiStringRes(R.string.my_site_btn_blog_posts),
                        onClick = ListItemInteraction.create(ListItemAction.POSTS, onClick)
                )
        )
        siteItems.add(
                ListItem(
                        R.drawable.ic_media_white_24dp,
                        UiStringRes(R.string.media),
                        onClick = ListItemInteraction.create(ListItemAction.MEDIA, onClick)
                )
        )
        siteItems.add(
                ListItem(
                        R.drawable.ic_comment_white_24dp,
                        UiStringRes(R.string.my_site_btn_comments),
                        onClick = ListItemInteraction.create(ListItemAction.COMMENTS, onClick)
                )
        )
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(site))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildThemesItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(site))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildPeopleItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildPluginItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildShareItemIfAvailable(site, onClick))
        siteItems.addItemIfNotNull(siteListItemBuilder.buildSiteSettingsItemIfAvailable(site, onClick))
        siteItems.add(CategoryHeader(UiStringRes(R.string.my_site_header_external)))
        siteItems.add(
                ListItem(
                        R.drawable.ic_globe_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_site),
                        secondaryIcon = R.drawable.ic_external_white_24dp,
                        onClick = ListItemInteraction.create(ListItemAction.VIEW_SITE, onClick)
                )
        )
        siteItems.addItemIfNotNull(siteListItemBuilder.buildAdminItemIfAvailable(site, onClick))
        return siteItems
    }

    private fun MutableList<MySiteItem>.addItemIfNotNull(item: MySiteItem?) {
        if (item != null) {
            this.add(item)
        }
    }
}
