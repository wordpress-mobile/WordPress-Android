package org.wordpress.android.ui.mysite.items

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder
import org.wordpress.android.ui.quickstart.QuickStartType

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock
    lateinit var siteListItemBuilder: SiteListItemBuilder

    @Mock
    lateinit var siteModel: SiteModel

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var quickStartType: QuickStartType

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartNewSiteTask.CHECK_STATS)
        siteItemsBuilder = SiteItemsBuilder(
            siteListItemBuilder,
            quickStartRepository,
            jetpackFeatureRemovalOverlayUtil
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
            addPlanItem = false,
            addPagesItem = true,
            addAdminItem = true,
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
            MANAGE_HEADER,
            ACTIVITY_ITEM,
            BACKUP_ITEM,
            SCAN_ITEM,
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
    fun `given new site QS stats task focus point enabled, when card built, then stats item focus point shown`() {
        setupHeaders()
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartNewSiteTask.CHECK_STATS)
        val enableStatsFocusPoint = true

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION,
                activeTask = QuickStartNewSiteTask.CHECK_STATS,
                enableFocusPoints = enableStatsFocusPoint
            )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
    }

    @Test
    fun `given new site QS stats task focus point disabled, when card built, then stats item focus point hidden`() {
        setupHeaders()
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartNewSiteTask.CHECK_STATS)
        val enableStatsFocusPoint = false

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION,
                activeTask = QuickStartNewSiteTask.CHECK_STATS,
                enableFocusPoints = enableStatsFocusPoint
            )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
    }

    @Test
    fun `given existing site QS stats task focus point enabled, when card built, then stats item focus point shown`() {
        setupHeaders()
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartExistingSiteTask.CHECK_STATS)
        val enableStatsFocusPoint = true

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION,
                activeTask = QuickStartExistingSiteTask.CHECK_STATS,
                enableFocusPoints = enableStatsFocusPoint
            )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
    }

    @Test
    fun `given existing site QS stats task focus point disabled, when card built, then stats item focus pt hidden`() {
        setupHeaders()
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartExistingSiteTask.CHECK_STATS)
        val enableStatsFocusPoint = false

        val buildSiteItems = siteItemsBuilder.build(
            SiteItemsBuilderParams(
                site = siteModel,
                onClick = SITE_ITEM_ACTION,
                activeTask = QuickStartExistingSiteTask.CHECK_STATS,
                enableFocusPoints = enableStatsFocusPoint
            )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
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
        addPlanItem: Boolean = false,
        addPagesItem: Boolean = false,
        addAdminItem: Boolean = false,
        addPeopleItem: Boolean = false,
        addPluginItem: Boolean = false,
        addShareItem: Boolean = false,
        addSiteDomainsItem: Boolean = false,
        addSiteSettingsItem: Boolean = false,
        addThemesItem: Boolean = false,
        addBackupItem: Boolean = false,
        addScanItem: Boolean = false,
        showPlansFocusPoint: Boolean = false,
        showPagesFocusPoint: Boolean = false
    ) {
        if (addPlanItem) {
            whenever(
                siteListItemBuilder.buildPlanItemIfAvailable(
                    siteModel,
                    showPlansFocusPoint,
                    SITE_ITEM_ACTION
                )
            ).thenReturn(
                PLAN_ITEM.copy(showFocusPoint = showPlansFocusPoint)
            )
        }
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
        if (addPagesItem) {
            whenever(
                siteListItemBuilder.buildPagesItemIfAvailable(
                    siteModel,
                    SITE_ITEM_ACTION,
                    showPagesFocusPoint
                )
            ).thenReturn(
                PAGES_ITEM.copy(showFocusPoint = showPagesFocusPoint)
            )
        }
        if (addAdminItem) {
            whenever(siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                ADMIN_ITEM
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
