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
    fun buildSiteItems(
        site: SiteModel,
        onClick: (ListItemAction) -> Unit,
        isBackupAvailable: Boolean = false,
        isScanAvailable: Boolean = false
    ): List<MySiteItem> {
        return listOfNotNull(
                siteListItemBuilder.buildPlanItemIfAvailable(site, onClick),
                siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(site),
                ListItem(
                        R.drawable.ic_stats_alt_white_24dp,
                        UiStringRes(R.string.stats),
                        onClick = ListItemInteraction.create(ListItemAction.STATS, onClick)
                ),
                siteListItemBuilder.buildActivityLogItemIfAvailable(site, onClick),
                siteListItemBuilder.buildBackupItemIfAvailable(onClick, isBackupAvailable),
                siteListItemBuilder.buildScanItemIfAvailable(onClick, isScanAvailable),
                siteListItemBuilder.buildJetpackItemIfAvailable(site, onClick),
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                ListItem(
                        R.drawable.ic_posts_white_24dp,
                        UiStringRes(R.string.my_site_btn_blog_posts),
                        onClick = ListItemInteraction.create(ListItemAction.POSTS, onClick)
                ),
                ListItem(
                        R.drawable.ic_media_white_24dp,
                        UiStringRes(R.string.media),
                        onClick = ListItemInteraction.create(ListItemAction.MEDIA, onClick)
                ),
                siteListItemBuilder.buildPagesItemIfAvailable(site, onClick),
                ListItem(
                        R.drawable.ic_comment_white_24dp,
                        UiStringRes(R.string.my_site_btn_comments),
                        onClick = ListItemInteraction.create(ListItemAction.COMMENTS, onClick)
                ),
                siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(site),
                siteListItemBuilder.buildThemesItemIfAvailable(site, onClick),
                siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(site),
                siteListItemBuilder.buildPeopleItemIfAvailable(site, onClick),
                siteListItemBuilder.buildPluginItemIfAvailable(site, onClick),
                siteListItemBuilder.buildShareItemIfAvailable(site, onClick),
                siteListItemBuilder.buildSiteSettingsItemIfAvailable(site, onClick),
                CategoryHeader(UiStringRes(R.string.my_site_header_external)),
                ListItem(
                        R.drawable.ic_globe_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_site),
                        secondaryIcon = R.drawable.ic_external_white_24dp,
                        onClick = ListItemInteraction.create(ListItemAction.VIEW_SITE, onClick)
                ),
                siteListItemBuilder.buildAdminItemIfAvailable(site, onClick)
        )
    }
}
