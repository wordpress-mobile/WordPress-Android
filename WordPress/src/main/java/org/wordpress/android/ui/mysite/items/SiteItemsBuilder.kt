package org.wordpress.android.ui.mysite.items

import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder @Inject constructor(
    private val siteListItemBuilder: SiteListItemBuilder,
    private val quickStartRepository: QuickStartRepository,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
) {
    fun build(params: InfoItemBuilderParams) = params.isStaleMessagePresent.takeIf { it }
            ?.let { InfoItem(title = UiStringRes(R.string.my_site_dashboard_stale_message)) }

    @Suppress("LongMethod")
    fun build(params: SiteItemsBuilderParams): List<MySiteCardAndItem> {
        val contentSiteItems = getContentSiteItems(params)
        val trafficSiteItems = getTrafficSiteItems(params)
        val manageSiteItems = getManageSiteItems(params)
        return contentSiteItems + trafficSiteItems + manageSiteItems
    }

    private fun getContentSiteItems(
        params: SiteItemsBuilderParams
    ): List<MySiteCardAndItem> {
        val showPagesFocusPoint = params.activeTask == QuickStartNewSiteTask.REVIEW_PAGES &&
                params.enablePagesFocusPoint
        val uploadMediaTask = quickStartRepository.quickStartType
                .getTaskFromString(QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL)
        val showMediaFocusPoint = params.activeTask == uploadMediaTask && params.enableMediaFocusPoint

        return listOfNotNull(
                CategoryHeaderItem(UiStringRes(string.my_site_header_content)),
            ListItem(
                drawable.ic_posts_white_24dp,
                UiStringRes(string.my_site_btn_blog_posts),
                onClick = ListItemInteraction.create(POSTS, params.onClick)
            ),
            siteListItemBuilder.buildPagesItemIfAvailable(params.site, params.onClick, showPagesFocusPoint),
            ListItem(
                drawable.ic_media_white_24dp,
                UiStringRes(string.media),
                onClick = ListItemInteraction.create(MEDIA, params.onClick),
                showFocusPoint = showMediaFocusPoint
            ),
            ListItem(
                drawable.ic_comment_white_24dp,
                UiStringRes(string.my_site_btn_comments),
                onClick = ListItemInteraction.create(COMMENTS, params.onClick)
            )
        )
    }

    private fun getTrafficSiteItems(
        params: SiteItemsBuilderParams
    ): List<MySiteCardAndItem> {
        val checkStatsTask = quickStartRepository.quickStartType
            .getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL)
        val showStatsFocusPoint = params.activeTask == checkStatsTask && params.enableStatsFocusPoint

        return listOfNotNull(
            CategoryHeaderItem(UiStringRes(string.my_site_header_traffic)),
            ListItem(
                R.drawable.ic_stats_alt_white_24dp,
                UiStringRes(R.string.stats),
                onClick = ListItemInteraction.create(ListItemAction.STATS, params.onClick),
                showFocusPoint = showStatsFocusPoint
            ),
            siteListItemBuilder.buildBlazeItemIfAvailable(params.isBlazeEligible, params.onClick)
        )
    }

    private fun getManageSiteItems(
        params: SiteItemsBuilderParams
    ): List<MySiteCardAndItem> {
        val header = CategoryHeaderItem(UiStringRes(string.my_site_header_manage))
        val activityLog = siteListItemBuilder.buildActivityLogItemIfAvailable(params.site, params.onClick)
        val backup = siteListItemBuilder.buildBackupItemIfAvailable(params.onClick, params.backupAvailable)
        val scan = siteListItemBuilder.buildScanItemIfAvailable(params.onClick, params.scanAvailable)

        val emptyHeaderItem1 = CategoryEmptyHeaderItem(UiString.UiStringText(""))
        val jetpackConfiguration = buildJetpackDependantConfigurationItemsIfNeeded(params)
        val lookAndFeel = getLookAndFeelSiteItems(params)
        val nonJetpackConfiguration = buildNonJetpackDependantConfigurationItemsIfNeeded(params)

        val emptyHeaderItem2 = CategoryEmptyHeaderItem(UiString.UiStringText(""))
        val admin = siteListItemBuilder.buildAdminItemIfAvailable(params.site, params.onClick)
        return listOfNotNull(header) +
                listOfNotNull(activityLog) +
                listOfNotNull(backup) +
                listOfNotNull(scan) +
                emptyHeaderItem1 +
                jetpackConfiguration +
                lookAndFeel +
                nonJetpackConfiguration +
                emptyHeaderItem2 +
                listOfNotNull(admin)
    }

    private fun getLookAndFeelSiteItems(params: SiteItemsBuilderParams): List<MySiteCardAndItem> {
        return if (!jetpackFeatureRemovalOverlayUtil.shouldHideJetpackFeatures())
            listOfNotNull(
                    siteListItemBuilder.buildThemesItemIfAvailable(params.site, params.onClick),
            ) else emptyList()
    }

    private fun buildNonJetpackDependantConfigurationItemsIfNeeded(params: SiteItemsBuilderParams):
            List<MySiteCardAndItem> {
        return listOfNotNull(
                siteListItemBuilder.buildDomainsItemIfAvailable(params.site, params.onClick),
            siteListItemBuilder.buildSiteSettingsItemIfAvailable(params.site, params.onClick)
        )
    }

    private fun buildJetpackDependantConfigurationItemsIfNeeded(params: SiteItemsBuilderParams):
            List<MySiteCardAndItem> {
        val showEnablePostSharingFocusPoint = params.activeTask == QuickStartNewSiteTask.ENABLE_POST_SHARING

        return if (!jetpackFeatureRemovalOverlayUtil.shouldHideJetpackFeatures()) {
            listOfNotNull(
                    siteListItemBuilder.buildPeopleItemIfAvailable(params.site, params.onClick),
                    siteListItemBuilder.buildPluginItemIfAvailable(params.site, params.onClick),
                    siteListItemBuilder.buildShareItemIfAvailable(
                            params.site,
                            params.onClick,
                            showEnablePostSharingFocusPoint
                    )
            )
        } else emptyList()
    }
}
