package org.wordpress.android.ui.mysite.items

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock
    lateinit var siteListItemBuilder: SiteListItemBuilder

    @Mock
    lateinit var siteModel: SiteModel

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        siteItemsBuilder = SiteItemsBuilder(
            siteListItemBuilder,
            jetpackFeatureRemovalOverlayUtil,
            experimentalFeatures
        )
    }

    @Test
    fun `always adds stats, publish, posts, media, comment, and view admin items`() {
        setupHeaders()

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION
            )
        )

        assertThat(buildSiteItems).containsExactly(
            CONTENT_HEADER,
            POSTS_ITEM,
            MEDIA_ITEM,
            COMMENTS_ITEM,
            TRAFFIC_HEADER,
            STATS_ITEM,
            MANAGE_HEADER,
            EMPTY_HEADER,
            EMPTY_HEADER
        )
    }

    @Test
    fun `adds all the items in the correct order`() {
        setupHeaders(
            addActivityLogItem = true,
            addSiteMonitoringItem = true,
            addPagesItem = true,
            addAdminItem = true,
            addSubscribersItem = true,
            addPeopleItem = true,
            addPluginItem = true,
            addShareItem = true,
            addSiteDomainsItem = true,
            addSiteSettingsItem = true,
            addThemesItem = true,
            addBackupItem = true,
            addScanItem = true
        )

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION
            )
        )

        assertThat(buildSiteItems).containsExactly(
            CONTENT_HEADER,
            POSTS_ITEM,
            PAGES_ITEM,
            MEDIA_ITEM,
            COMMENTS_ITEM,
            TRAFFIC_HEADER,
            STATS_ITEM,
            SUBSCRIBERS_ITEM,
            MANAGE_HEADER,
            ACTIVITY_ITEM,
            BACKUP_ITEM,
            SCAN_ITEM,
            SITE_MONITORING_ITEM,
            EMPTY_HEADER,
            PEOPLE_ITEM,
            PLUGINS_ITEM,
            SHARING_ITEM,
            THEMES_ITEM,
            DOMAINS_ITEM,
            SITE_SETTINGS_ITEM,
            EMPTY_HEADER,
            ADMIN_ITEM
        )
    }

    @Test
    fun `given site domains flag is not enabled, when build site domains is invoked, then site domains is built`() {
        whenever(siteListItemBuilder.buildDomainsItemIfAvailable(siteModel, SITE_ITEM_ACTION))
            .thenReturn(null)

        val siteDomainsItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION
            )
        )

        assertThat(siteDomainsItems).doesNotContain(DOMAINS_ITEM)
    }

    @Test
    fun `given site domains flag is enabled, when build site domains is invoked, then site domains is built`() {
        whenever(siteListItemBuilder.buildDomainsItemIfAvailable(siteModel, SITE_ITEM_ACTION))
            .thenReturn(DOMAINS_ITEM)

        val siteDomainsItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION
            )
        )

        assertThat(siteDomainsItems).contains(DOMAINS_ITEM)
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun setupHeaders(
        addActivityLogItem: Boolean = false,
        addSiteMonitoringItem: Boolean = false,
        addPagesItem: Boolean = false,
        addAdminItem: Boolean = false,
        addSubscribersItem: Boolean = false,
        addPeopleItem: Boolean = false,
        addPluginItem: Boolean = false,
        addShareItem: Boolean = false,
        addSiteDomainsItem: Boolean = false,
        addSiteSettingsItem: Boolean = false,
        addThemesItem: Boolean = false,
        addBackupItem: Boolean = false,
        addScanItem: Boolean = false
    ) {
        if (addActivityLogItem) {
            whenever(siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                ACTIVITY_ITEM
            )
        }
        if (addBackupItem) {
            whenever(siteListItemBuilder.buildBackupItemIfAvailable(SITE_ITEM_ACTION)).thenReturn(
                BACKUP_ITEM
            )
        }
        if (addScanItem) {
            whenever(siteListItemBuilder.buildScanItemIfAvailable(SITE_ITEM_ACTION)).thenReturn(
                SCAN_ITEM
            )
        }
        if (addSiteMonitoringItem) {
            whenever(siteListItemBuilder.buildSiteMonitoringItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                SITE_MONITORING_ITEM
            )
        }
        if (addPagesItem) {
            whenever(
                siteListItemBuilder.buildPagesItemIfAvailable(
                    siteModel,
                    SITE_ITEM_ACTION
                )
            ).thenReturn(PAGES_ITEM)
        }
        if (addAdminItem) {
            whenever(siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                ADMIN_ITEM
            )
        }
        if (addSubscribersItem) {
            whenever(siteListItemBuilder.buildSubscribersItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                SUBSCRIBERS_ITEM
            )
        }
        if (addPeopleItem) {
            whenever(siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                PEOPLE_ITEM
            )
        }
        if (addPluginItem) {
            whenever(siteListItemBuilder.buildPluginItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                PLUGINS_ITEM
            )
        }
        if (addShareItem) {
            whenever(siteListItemBuilder.buildShareItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                SHARING_ITEM
            )
        }
        if (addSiteSettingsItem) {
            whenever(siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                SITE_SETTINGS_ITEM
            )
        }
        if (addThemesItem) {
            whenever(siteListItemBuilder.buildThemesItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                THEMES_ITEM
            )
        }
        if (addSiteDomainsItem) {
            whenever(siteListItemBuilder.buildDomainsItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                DOMAINS_ITEM
            )
        }
    }
}
