package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock lateinit var siteCategoryItemBuilder: SiteCategoryItemBuilder
    @Mock lateinit var siteListItemBuilder: SiteListItemBuilder
    @Mock lateinit var siteModel: SiteModel
    private val statsItem = ListItem(R.drawable.ic_stats_alt_white_24dp, UiStringRes(R.string.stats), onClick = onClick)
    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        siteItemsBuilder = SiteItemsBuilder(siteCategoryItemBuilder, siteListItemBuilder)
    }

    @Test
    fun `always adds stats, publish, posts, media, comment, external and view site items`() {
        setupHeaders(addJetpackHeader = false, addLookAndFeelHeader = false, addConfigurationHeader = false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel, onClick)

        assertThat(buildSiteItems).containsExactly(
                statsItem,
                publishHeader,
                postsItem,
                mediaItem,
                commentsItem,
                externalHeader,
                viewSiteItem
        )
    }

    @Test
    fun `always all the items in the correct order`() {
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
                addScanItem = true
        )

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel, onClick)

        assertThat(buildSiteItems).containsExactly(
                planItem,
                jetpackHeader,
                statsItem,
                activityItem,
                scanItem,
                jetpackItem,
                publishHeader,
                pagesItem,
                postsItem,
                mediaItem,
                commentsItem,
                lookAndFeelHeader,
                themesItem,
                configurationHeader,
                peopleItem,
                pluginsItem,
                sharingItem,
                siteSettingsItem,
                externalHeader,
                viewSiteItem,
                adminItem
        )
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
        addScanItem: Boolean = false
    ) {
        if (addJetpackHeader) {
            whenever(siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)).thenReturn(
                    jetpackHeader
            )
        }
        if (addJetpackSettings) {
            whenever(siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, onClick)).thenReturn(
                    jetpackItem
            )
        }
        if (addLookAndFeelHeader) {
            whenever(siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(siteModel)).thenReturn(
                    lookAndFeelHeader
            )
        }
        if (addConfigurationHeader) {
            whenever(siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)).thenReturn(
                    configurationHeader
            )
        }
        if (addPlanItem) {
            whenever(siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)).thenReturn(
                    planItem
            )
        }
        if (addActivityLogItem) {
            whenever(siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)).thenReturn(
                    activityItem
            )
        }
        if (addScanItem) {
            whenever(siteListItemBuilder.buildScanItemIfAvailable(onClick)).thenReturn(
                    scanItem
            )
        }
        if (addPagesItem) {
            whenever(siteListItemBuilder.buildPagesItemIfAvailable(siteModel, onClick)).thenReturn(
                    pagesItem
            )
        }
        if (addAdminItem) {
            whenever(siteListItemBuilder.buildAdminItemIfAvailable(siteModel, onClick)).thenReturn(
                    adminItem
            )
        }
        if (addPeopleItem) {
            whenever(siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, onClick)).thenReturn(
                    peopleItem
            )
        }
        if (addPluginItem) {
            whenever(siteListItemBuilder.buildPluginItemIfAvailable(siteModel, onClick)).thenReturn(
                    pluginsItem
            )
        }
        if (addShareItem) {
            whenever(siteListItemBuilder.buildShareItemIfAvailable(siteModel, onClick)).thenReturn(
                    sharingItem
            )
        }
        if (addSiteSettingsItem) {
            whenever(siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, onClick)).thenReturn(
                    siteSettingsItem
            )
        }
        if (addThemesItem) {
            whenever(siteListItemBuilder.buildThemesItemIfAvailable(siteModel, onClick)).thenReturn(
                    themesItem
            )
        }
    }
}
