package org.wordpress.android.ui.mysite.items

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.items.categoryheader.SiteCategoryItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder
@Inject constructor(
    private val siteCategoryItemBuilder: SiteCategoryItemBuilder,
    private val siteListItemBuilder: SiteListItemBuilder
) {
    @Suppress("LongParameterList")
    fun buildSiteItems(
        site: SiteModel,
        onClick: (ListItemAction) -> Unit,
        isBackupAvailable: Boolean = false,
        isScanAvailable: Boolean = false,
        showViewSiteFocusPoint: Boolean = false,
        showEnablePostSharingFocusPoint: Boolean = false,
        showExplorePlansFocusPoint: Boolean = false
    ): List<MySiteCardAndItem> {
        return listOfNotNull(
                siteListItemBuilder.buildPlanItemIfAvailable(site, showExplorePlansFocusPoint, onClick),
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
                CategoryHeaderItem(UiStringRes(R.string.my_site_header_publish)),
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
                siteListItemBuilder.buildShareItemIfAvailable(site, onClick, showEnablePostSharingFocusPoint),
                siteListItemBuilder.buildDomainsItemIfAvailable(site, onClick),
                siteListItemBuilder.buildSiteSettingsItemIfAvailable(site, onClick),
                CategoryHeaderItem(UiStringRes(R.string.my_site_header_external)),
                ListItem(
                        R.drawable.ic_globe_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_site),
                        secondaryIcon = R.drawable.ic_external_white_24dp,
                        onClick = ListItemInteraction.create(ListItemAction.VIEW_SITE, onClick),
                        showFocusPoint = showViewSiteFocusPoint
                ),
                siteListItemBuilder.buildAdminItemIfAvailable(site, onClick)
        )
    }
}
