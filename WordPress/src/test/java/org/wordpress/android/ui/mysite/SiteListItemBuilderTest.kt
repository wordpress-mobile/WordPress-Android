package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.plugins.PluginUtilsWrapper
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.ScanFeatureConfig
import org.wordpress.android.util.SiteUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class SiteListItemBuilderTest {
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var pluginUtilsWrapper: PluginUtilsWrapper
    @Mock lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Mock lateinit var scanFeatureConfig: ScanFeatureConfig
    @Mock lateinit var themeBrowserUtils: ThemeBrowserUtils
    @Mock lateinit var siteModel: SiteModel
    @Mock lateinit var onClick: ListItemInteraction
    private val business = "business"
    private lateinit var siteListItemBuilder: SiteListItemBuilder

    @Before
    fun setUp() {
        siteListItemBuilder = SiteListItemBuilder(
                accountStore,
                pluginUtilsWrapper,
                siteUtilsWrapper,
                scanFeatureConfig,
                themeBrowserUtils
        )
    }

    @Test
    fun `activity item built when site uses WPComRest, can manage options and is not WP for teams`() {
        setupActivityLogItem(
                isAccessedViaWPComRest = true,
                canManageOptions = true,
                isJetpackConnected = false,
                isWpForTeams = false
        )

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_history_alt_white_24dp,
                        UiStringRes(R.string.activity),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `activity item built when site is Jetpack, can manage options and is not WP for teams`() {
        setupActivityLogItem(
                isAccessedViaWPComRest = false,
                canManageOptions = true,
                isJetpackConnected = true,
                isWpForTeams = false
        )

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_history_alt_white_24dp,
                        UiStringRes(R.string.activity),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `activity item not built when site cannot manage options`() {
        setupActivityLogItem(canManageOptions = false)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `activity item not built when site is WP for teams`() {
        setupActivityLogItem(isWpForTeams = true)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `activity item not built when site is neither Jetpack nor uses WPComRest`() {
        setupActivityLogItem(isAccessedViaWPComRest = false, isJetpackConnected = false)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    private fun setupActivityLogItem(
        isAccessedViaWPComRest: Boolean = true,
        isJetpackConnected: Boolean = true,
        canManageOptions: Boolean = true,
        isWpForTeams: Boolean = false
    ) {
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(isAccessedViaWPComRest)
        whenever(siteModel.isJetpackConnected).thenReturn(isJetpackConnected)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
        whenever(siteModel.isWpForTeamsSite).thenReturn(isWpForTeams)
    }

    @Test
    fun `scan item built if scan feature config enabled`() {
        whenever(scanFeatureConfig.isEnabled()).thenReturn(true)

        val item = siteListItemBuilder.buildScanItemIfAvailable(onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_scan_alt_white_24dp,
                        UiStringRes(R.string.scan),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `scan item not built if scan feature config not enabled`() {
        whenever(scanFeatureConfig.isEnabled()).thenReturn(false)

        val item = siteListItemBuilder.buildScanItemIfAvailable(onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `plan item built when plan not empty, site can manage options, is not WP for teams and is WPcom`() {
        setupPlanItem(
                planShortName = business,
                canManageOptions = true,
                isWpForTeams = false,
                isWPCom = true,
                isAutomatedTransfer = false
        )

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_plans_white_24dp,
                        UiStringRes(R.string.plan),
                        secondaryText = UiStringText(business),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `plan item built when plan not empty, site can manage options, is not WP for teams and is AT`() {
        setupPlanItem(
                planShortName = business,
                canManageOptions = true,
                isWpForTeams = false,
                isWPCom = false,
                isAutomatedTransfer = true
        )

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_plans_white_24dp,
                        UiStringRes(R.string.plan),
                        secondaryText = UiStringText(business),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `plan item not built when plan name is empty`() {
        setupPlanItem(planShortName = "")

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `plan item not built when site cannot manage options`() {
        setupPlanItem(canManageOptions = false)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `plan item not built when site is WP for teams`() {
        setupPlanItem(isWpForTeams = true)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `plan item not built when site is neither WP com nor AT`() {
        setupPlanItem(isWPCom = false, isAutomatedTransfer = false)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    private fun setupPlanItem(
        planShortName: String = business,
        canManageOptions: Boolean = true,
        isWpForTeams: Boolean = false,
        isWPCom: Boolean = true,
        isAutomatedTransfer: Boolean = true
    ) {
        whenever(siteModel.planShortName).thenReturn(planShortName)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
        whenever(siteModel.isWpForTeamsSite).thenReturn(isWpForTeams)
        whenever(siteModel.isWPCom).thenReturn(isWPCom)
        whenever(siteModel.isAutomatedTransfer).thenReturn(isAutomatedTransfer)
    }

    @Test
    fun `pages item not built when not self-hosted admin and cannot edit pages`() {
        setupPagesItem(isSelfHostedAdmin = false, canEditPages = false)

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `pages item built when self-hosted admin`() {
        setupPagesItem(isSelfHostedAdmin = true)

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_pages_white_24dp,
                        UiStringRes(R.string.my_site_btn_site_pages),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `pages item built when can edit pages`() {
        setupPagesItem(canEditPages = true)

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_pages_white_24dp,
                        UiStringRes(R.string.my_site_btn_site_pages),
                        onClick = onClick
                )
        )
    }

    private fun setupPagesItem(isSelfHostedAdmin: Boolean = false, canEditPages: Boolean = false) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(isSelfHostedAdmin)
        whenever(siteModel.hasCapabilityEditPages).thenReturn(canEditPages)
    }

    @Test
    fun `admin item built when site is not WPCom`() {
        setupAdminItem(isWPCom = false)

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_my_sites_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_admin),
                        isExternalLink = true,
                        onClick = onClick
                )
        )
    }

    @Test
    fun `admin item built when site is WPCom and account created before 2015-10-07`() {
        setupAdminItem(isWPCom = true, accountDate = "2015-10-06T02:00:00+0200")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_my_sites_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_admin),
                        isExternalLink = true,
                        onClick = onClick
                )
        )
    }

    @Test
    fun `admin item built when site is WPCom and account date is empty`() {
        setupAdminItem(isWPCom = true, accountDate = "")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_my_sites_white_24dp,
                        UiStringRes(R.string.my_site_btn_view_admin),
                        isExternalLink = true,
                        onClick = onClick
                )
        )
    }

    @Test
    fun `admin item not built when site is WPCom and account created after 2015-10-07`() {
        setupAdminItem(isWPCom = true, accountDate = "2015-10-08T02:00:00+0200")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    private fun setupAdminItem(isWPCom: Boolean = true, accountDate: String = "") {
        whenever(siteModel.isWPCom).thenReturn(isWPCom)
        val accountModel = AccountModel()
        accountModel.date = accountDate
        whenever(accountStore.account).thenReturn(accountModel)
    }

    @Test
    fun `people item built if site can list users`() {
        whenever(siteModel.hasCapabilityListUsers).thenReturn(true)

        val item = siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_user_white_24dp,
                        UiStringRes(R.string.people),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `people item not built if site cannot list users`() {
        whenever(siteModel.hasCapabilityListUsers).thenReturn(false)

        val item = siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `plugin item built if feature available`() {
        whenever(pluginUtilsWrapper.isPluginFeatureAvailable(siteModel)).thenReturn(true)

        val item = siteListItemBuilder.buildPluginItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_plugins_white_24dp,
                        UiStringRes(R.string.my_site_btn_plugins),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `plugin item not built if feature not available`() {
        whenever(pluginUtilsWrapper.isPluginFeatureAvailable(siteModel)).thenReturn(false)

        val item = siteListItemBuilder.buildPluginItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `share item built if is accessed through WPCom REST`() {
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(true)

        val item = siteListItemBuilder.buildShareItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_share_white_24dp,
                        UiStringRes(R.string.my_site_btn_sharing),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `share item not built if is not accessed through WPCom REST`() {
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(false)

        val item = siteListItemBuilder.buildShareItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    @Test
    fun `settings item built if site can manage options`() {
        setupSiteSettings(canManageOptions = true)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_cog_white_24dp,
                        UiStringRes(R.string.my_site_btn_site_settings),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `settings item built if site is not accessed through WPCom REST`() {
        setupSiteSettings(isAccessedViaWPComRest = false)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, onClick)

        assertThat(item).isEqualTo(
                ListItem(
                        R.drawable.ic_cog_white_24dp,
                        UiStringRes(R.string.my_site_btn_site_settings),
                        onClick = onClick
                )
        )
    }

    @Test
    fun `settings item not built if site is accessed through WPCom REST and cannot manage options`() {
        setupSiteSettings(canManageOptions = false, isAccessedViaWPComRest = true)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, onClick)

        assertThat(item).isNull()
    }

    private fun setupSiteSettings(canManageOptions: Boolean = false, isAccessedViaWPComRest: Boolean = true) {
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(isAccessedViaWPComRest)
    }
}
