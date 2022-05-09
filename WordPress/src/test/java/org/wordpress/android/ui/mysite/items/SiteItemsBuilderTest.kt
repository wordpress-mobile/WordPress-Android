package org.wordpress.android.ui.mysite.items

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CHECK_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EXPLORE_PLANS
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
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
        siteItemsBuilder = SiteItemsBuilder(
                siteCategoryItemBuilder,
                siteListItemBuilder
        )
    }

    @Test
    fun `always adds stats, publish, posts, media, comment, external and view site items`() {
        setupHeaders(addJetpackHeader = false, addLookAndFeelHeader = false, addConfigurationHeader = false)

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION
                )
        )

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
                addPlanItem = false,
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

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION
                )
        )

        assertThat(buildSiteItems).containsExactly(
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

    /* QUICK START - FOCUS POINT */

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `passes parameter to show focus point to plan item`() {
        val showPlansFocusPoint = true
        setupHeaders(addPlanItem = true, showPlansFocusPoint = showPlansFocusPoint)

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION,
                        activeTask = EXPLORE_PLANS
                )
        )

        assertThat(buildSiteItems.first()).isEqualTo(PLAN_ITEM.copy(showFocusPoint = showPlansFocusPoint))
    }

    @Test
    fun `given pages focus point enabled, when card built, showFocusPoint should be true`() {
        val showPagesFocusPoint = true
        setupHeaders(addPagesItem = true, showPagesFocusPoint = showPagesFocusPoint)

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION,
                        activeTask = EDIT_HOMEPAGE,
                        enablePagesFocusPoint = showPagesFocusPoint
                )
        )

        assertThat(buildSiteItems).contains(PAGES_ITEM.copy(showFocusPoint = showPagesFocusPoint))
    }

    @Test
    fun `given stats focus point enabled, when card built, then showFocusPoint should be true`() {
        setupHeaders()
        val enableStatsFocusPoint = true

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION,
                        activeTask = CHECK_STATS,
                        enableStatsFocusPoint = enableStatsFocusPoint
                )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
    }

    @Test
    fun `given stats focus point disabled, when card built, then showFocusPoint should be false`() {
        setupHeaders()
        val enableStatsFocusPoint = false

        val buildSiteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = siteModel,
                        onClick = SITE_ITEM_ACTION,
                        activeTask = CHECK_STATS,
                        enableStatsFocusPoint = enableStatsFocusPoint
                )
        )

        assertThat(buildSiteItems).contains(STATS_ITEM.copy(showFocusPoint = enableStatsFocusPoint))
    }

    /* INFO ITEM */

    @Test
    fun `when build info item is invoked, then info item is built`() {
        val infoItem = siteItemsBuilder.build(
                InfoItemBuilderParams(
                        isStaleMessagePresent = true
                )
        )

        assertThat(infoItem).isNotNull
    }

    @Test
    fun `given stale message present, when build info item is invoked, then info item is built`() {
        val infoItem = siteItemsBuilder.build(
                InfoItemBuilderParams(
                        isStaleMessagePresent = true
                )
        )

        assertThat(infoItem).isNotNull
    }

    @Test
    fun `given stale message not present, when build info item is invoked, then info item is not built`() {
        val infoItem = siteItemsBuilder.build(
                InfoItemBuilderParams(
                        isStaleMessagePresent = false
                )
        )

        assertThat(infoItem).isNull()
    }

    @Suppress("ComplexMethod", "LongMethod", "LongParameterList")
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
        showPlansFocusPoint: Boolean = false,
        showPagesFocusPoint: Boolean = false
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
    }
}
