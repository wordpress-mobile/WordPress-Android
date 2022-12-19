package org.wordpress.android.ui.mysite.items

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.items.categoryheader.SiteCategoryItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder @Inject constructor(
    private val siteCategoryItemBuilder: SiteCategoryItemBuilder,
    private val siteListItemBuilder: SiteListItemBuilder,
    private val quickStartRepository: QuickStartRepository
) {
    fun build(params: InfoItemBuilderParams) = params.isStaleMessagePresent.takeIf { it }
                ?.let { InfoItem(title = UiStringRes(R.string.my_site_dashboard_stale_message)) }

    @Suppress("LongMethod")
    fun build(params: SiteItemsBuilderParams): List<MySiteCardAndItem> {
        val showEnablePostSharingFocusPoint = params.activeTask == QuickStartNewSiteTask.ENABLE_POST_SHARING
        val checkStatsTask = quickStartRepository.quickStartType
                .getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL)
        val showStatsFocusPoint = params.activeTask == checkStatsTask && params.enableStatsFocusPoint
        val showPagesFocusPoint = params.activeTask == QuickStartNewSiteTask.REVIEW_PAGES &&
                params.enablePagesFocusPoint
        val uploadMediaTask = quickStartRepository.quickStartType
                .getTaskFromString(QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL)
        val showMediaFocusPoint = params.activeTask == uploadMediaTask && params.enableMediaFocusPoint

        return listOfNotNull(
                siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(params.site),
                ListItem(
                        R.drawable.ic_stats_alt_white_24dp,
                        UiStringRes(R.string.stats),
                        onClick = ListItemInteraction.create(ListItemAction.STATS, params.onClick),
                        showFocusPoint = showStatsFocusPoint
                ),
                siteListItemBuilder.buildActivityLogItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildBackupItemIfAvailable(params.onClick, params.backupAvailable),
                siteListItemBuilder.buildScanItemIfAvailable(params.onClick, params.scanAvailable),
                siteListItemBuilder.buildJetpackItemIfAvailable(params.site, params.onClick),
                CategoryHeaderItem(UiStringRes(R.string.my_site_header_publish)),
                ListItem(
                        R.drawable.ic_posts_white_24dp,
                        UiStringRes(R.string.my_site_btn_blog_posts),
                        onClick = ListItemInteraction.create(ListItemAction.POSTS, params.onClick)
                ),
                ListItem(
                        R.drawable.ic_media_white_24dp,
                        UiStringRes(R.string.media),
                        onClick = ListItemInteraction.create(ListItemAction.MEDIA, params.onClick),
                        showFocusPoint = showMediaFocusPoint
                ),
                siteListItemBuilder.buildPagesItemIfAvailable(params.site, params.onClick, showPagesFocusPoint),
                ListItem(
                        R.drawable.ic_comment_white_24dp,
                        UiStringRes(R.string.my_site_btn_comments),
                        onClick = ListItemInteraction.create(ListItemAction.COMMENTS, params.onClick)
                ),
                siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(params.site),
                siteListItemBuilder.buildThemesItemIfAvailable(params.site, params.onClick),
                siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(params.site),
                siteListItemBuilder.buildPeopleItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildPluginItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildShareItemIfAvailable(
                        params.site,
                        params.onClick,
                        showEnablePostSharingFocusPoint
                ),
                siteListItemBuilder.buildDomainsItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildSiteSettingsItemIfAvailable(params.site, params.onClick),
                CategoryHeaderItem(UiStringRes(R.string.my_site_header_external)),
                ListItem(
                        R.drawable.ic_globe_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_site),
                        secondaryIcon = R.drawable.ic_external_white_24dp,
                        onClick = ListItemInteraction.create(ListItemAction.VIEW_SITE, params.onClick)
                ),
                siteListItemBuilder.buildAdminItemIfAvailable(params.site, params.onClick)
        )
    }
}
