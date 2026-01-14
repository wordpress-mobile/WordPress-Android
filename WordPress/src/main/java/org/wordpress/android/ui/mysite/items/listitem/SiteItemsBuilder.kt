package org.wordpress.android.ui.mysite.items.listitem

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POST_TYPES
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SiteItemsBuilder @Inject constructor(
    private val siteListItemBuilder: SiteListItemBuilder,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val experimentalFeatures: ExperimentalFeatures
) {
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
        return listOfNotNull(
            CategoryHeaderItem(UiStringRes(R.string.my_site_header_content)),
            ListItem(
                R.drawable.ic_posts_white_24dp,
                UiStringRes(R.string.my_site_btn_blog_posts),
                onClick = ListItemInteraction.create(POSTS, params.onClick),
                listItemAction = POSTS
            ),
            siteListItemBuilder.buildPagesItemIfAvailable(params.site, params.onClick),
            buildPostTypesItemIfEnabled(params.onClick),
            ListItem(
                R.drawable.ic_media_white_24dp,
                UiStringRes(R.string.media),
                onClick = ListItemInteraction.create(MEDIA, params.onClick),
                listItemAction = MEDIA
            ),
            ListItem(
                R.drawable.ic_comment_white_24dp,
                UiStringRes(R.string.my_site_btn_comments),
                onClick = ListItemInteraction.create(COMMENTS, params.onClick),
                listItemAction = COMMENTS
            )
        )
    }

    private fun buildPostTypesItemIfEnabled(onClick: (ListItemAction) -> Unit): ListItem? {
        return if (experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_POST_TYPES)) {
            ListItem(
                R.drawable.ic_posts_white_24dp,
                UiStringRes(R.string.post_types_screen_title),
                onClick = ListItemInteraction.create(POST_TYPES, onClick),
                listItemAction = POST_TYPES
            )
        } else {
            null
        }
    }

    private fun getTrafficSiteItems(
        params: SiteItemsBuilderParams
    ): List<MySiteCardAndItem> {
        if (jetpackFeatureRemovalOverlayUtil.shouldHideJetpackFeatures())
            return emptyList()

        return listOfNotNull(
            CategoryHeaderItem(UiStringRes(R.string.my_site_header_traffic)),
            ListItem(
                R.drawable.ic_stats_alt_white_24dp,
                UiStringRes(R.string.stats),
                onClick = ListItemInteraction.create(ListItemAction.STATS, params.onClick),
                listItemAction = ListItemAction.STATS
            ),
            siteListItemBuilder.buildSubscribersItemIfAvailable(params.site, params.onClick),
            siteListItemBuilder.buildBlazeItemIfAvailable(params.isBlazeEligible, params.onClick)
        )
    }

    private fun getManageSiteItems(
        params: SiteItemsBuilderParams
    ): List<MySiteCardAndItem> {
        val manageSiteItems = buildManageSiteItems(params)
        val siteMonitoring = buildSiteMonitoringOptionsIfNeeded(params)

        val emptyHeaderItem1 = CategoryEmptyHeaderItem(UiString.UiStringText(""))
        val jetpackConfiguration = buildJetpackDependantConfigurationItemsIfNeeded(params)
        val lookAndFeel = getLookAndFeelSiteItems(params)
        val nonJetpackConfiguration = buildNonJetpackDependantConfigurationItemsIfNeeded(params)
        val manageHeader = CategoryHeaderItem(UiStringRes(R.string.my_site_header_manage))
        val emptyHeaderItem2 = CategoryEmptyHeaderItem(UiString.UiStringText(""))
        val admin = siteListItemBuilder.buildAdminItemIfAvailable(params.site, params.onClick)
        return listOf(manageHeader) +
                manageSiteItems +
                siteMonitoring +
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
            siteListItemBuilder.buildMeItemIfAvailable(params.site, params.onClick),
            siteListItemBuilder.buildSiteSettingsItemIfAvailable(params.site, params.onClick),
            siteListItemBuilder.buildApplicationPasswordsItemIfAvailable(params.site, params.onClick),
        )
    }

    private fun buildSiteMonitoringOptionsIfNeeded(params: SiteItemsBuilderParams): List<MySiteCardAndItem> {
        return listOfNotNull(
            siteListItemBuilder.buildSiteMonitoringItemIfAvailable(params.site, params.onClick)
        )
    }

    private fun buildManageSiteItems(params: SiteItemsBuilderParams): List<MySiteCardAndItem>{
        if(jetpackFeatureRemovalOverlayUtil.shouldHideJetpackFeatures())
            return emptyList()
        return listOfNotNull(
            siteListItemBuilder.buildActivityLogItemIfAvailable(params.site, params.onClick),
            siteListItemBuilder.buildBackupItemIfAvailable(params.onClick, params.backupAvailable),
            siteListItemBuilder.buildScanItemIfAvailable(params.onClick, params.scanAvailable),
        )
    }

    private fun buildJetpackDependantConfigurationItemsIfNeeded(params: SiteItemsBuilderParams):
            List<MySiteCardAndItem> {
        return if (!jetpackFeatureRemovalOverlayUtil.shouldHideJetpackFeatures()) {
            listOfNotNull(
                siteListItemBuilder.buildPeopleItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildSelfHostedUserListItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildPluginItemIfAvailable(params.site, params.onClick),
                siteListItemBuilder.buildShareItemIfAvailable(params.site, params.onClick)
            )
        } else emptyList()
    }
}
