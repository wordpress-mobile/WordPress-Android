package org.wordpress.android.ui.mysite.items

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.items.categoryheader.SiteCategoryItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteListItemBuilder

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock lateinit var siteCategoryItemBuilder: SiteCategoryItemBuilder
    @Mock lateinit var siteListItemBuilder: SiteListItemBuilder
    @Mock lateinit var siteModel: SiteModel
    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        siteItemsBuilder = SiteItemsBuilder(siteCategoryItemBuilder, siteListItemBuilder)
    }

    @Test
    fun `always adds stats, publish, posts, media, comment, external and view site items`() {
        setupHeaders(addJetpackHeader = false, addLookAndFeelHeader = false, addConfigurationHeader = false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel, SITE_ITEM_ACTION)

        assertThat(buildSiteItems).containsExactly(
                STATS_ITEM,
                PUBLISH_HEADER,
                POSTS_ITEM,
                MEDIA_ITEM,
                COMMENTS_ITEM,
                EXTERNAL_HEADER,
                VIEW_SITE_ITEM
        )
    }

    @Test
    fun `adds all the items in the correct order`() {
        setupHeaders(
                addJetpackHeader = true,
                addJetpackSettings = true,
                addLookAndFeelHeader = true,
                addConfigurationHeader = true,
                addActivityLogItem = true,
                addPlanItem = true,
                addPagesItem = true,
                addAdminItem = true,
                addPeopleItem = true,
                addPluginItem = true,
                addShareItem = true,
                addSiteSettingsItem = true,
                addThemesItem = true,
                addBackupItem = true,
                addScanItem = true
        )

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel, SITE_ITEM_ACTION)

        assertThat(buildSiteItems).containsExactly(
                PLAN_ITEM,
                JETPACK_HEADER,
                STATS_ITEM,
                ACTIVITY_ITEM,
                BACKUP_ITEM,
                SCAN_ITEM,
                JETPACK_ITEM,
                PUBLISH_HEADER,
                POSTS_ITEM,
                MEDIA_ITEM,
                PAGES_ITEM,
                COMMENTS_ITEM,
                LOOK_AND_FEEL_HEADER,
                THEMES_ITEM,
                CONFIGURATION_HEADER,
                PEOPLE_ITEM,
                PLUGINS_ITEM,
                SHARING_ITEM,
                SITE_SETTINGS_ITEM,
                EXTERNAL_HEADER,
                VIEW_SITE_ITEM,
                ADMIN_ITEM
        )
    }

    @Test
    fun `passes parameter to show focus point to plan item`() {
        val showPlansFocusPoint = true
        setupHeaders(addPlanItem = true, showPlansFocusPoint = showPlansFocusPoint)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(
                siteModel,
                SITE_ITEM_ACTION,
                showExplorePlansFocusPoint = showPlansFocusPoint
        )

        assertThat(buildSiteItems.first()).isEqualTo(PLAN_ITEM.copy(showFocusPoint = showPlansFocusPoint))
    }

    private fun setupHeaders(
        addJetpackHeader: Boolean = false,
        addJetpackSettings: Boolean = false,
        addLookAndFeelHeader: Boolean = false,
        addConfigurationHeader: Boolean = false,
        addActivityLogItem: Boolean = false,
        addPlanItem: Boolean = false,
        addPagesItem: Boolean = false,
        addAdminItem: Boolean = false,
        addPeopleItem: Boolean = false,
        addPluginItem: Boolean = false,
        addShareItem: Boolean = false,
        addSiteSettingsItem: Boolean = false,
        addThemesItem: Boolean = false,
        addBackupItem: Boolean = false,
        addScanItem: Boolean = false,
        showPlansFocusPoint: Boolean = false
    ) {
        if (addJetpackHeader) {
            whenever(siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)).thenReturn(
                    JETPACK_HEADER
            )
        }
        if (addJetpackSettings) {
            whenever(siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                    JETPACK_ITEM
            )
        }
        if (addLookAndFeelHeader) {
            whenever(siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(siteModel)).thenReturn(
                    LOOK_AND_FEEL_HEADER
            )
        }
        if (addConfigurationHeader) {
            whenever(siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)).thenReturn(
                    CONFIGURATION_HEADER
            )
        }
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
            whenever(siteListItemBuilder.buildPagesItemIfAvailable(siteModel, SITE_ITEM_ACTION)).thenReturn(
                    PAGES_ITEM
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
    }
}
