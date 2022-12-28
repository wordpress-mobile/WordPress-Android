package org.wordpress.android.ui.mysite.items.listitem

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.items.ACTIVITY_ITEM
import org.wordpress.android.ui.mysite.items.ADMIN_ITEM
import org.wordpress.android.ui.mysite.items.BACKUP_ITEM
import org.wordpress.android.ui.mysite.items.JETPACK_ITEM
import org.wordpress.android.ui.mysite.items.PAGES_ITEM
import org.wordpress.android.ui.mysite.items.PEOPLE_ITEM
import org.wordpress.android.ui.mysite.items.PLAN_ITEM
import org.wordpress.android.ui.mysite.items.PLAN_NAME
import org.wordpress.android.ui.mysite.items.PLUGINS_ITEM
import org.wordpress.android.ui.mysite.items.SCAN_ITEM
import org.wordpress.android.ui.mysite.items.SHARING_ITEM
import org.wordpress.android.ui.mysite.items.SITE_ITEM_ACTION
import org.wordpress.android.ui.mysite.items.SITE_SETTINGS_ITEM
import org.wordpress.android.ui.plugins.PluginUtilsWrapper
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class SiteListItemBuilderTest {
    @Mock
    lateinit var accountStore: AccountStore
    @Mock
    lateinit var pluginUtilsWrapper: PluginUtilsWrapper
    @Mock
    lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock
    lateinit var themeBrowserUtils: ThemeBrowserUtils
    @Mock
    lateinit var siteModel: SiteModel
    @Mock
    lateinit var siteDomainsFeatureConfig: SiteDomainsFeatureConfig
    private lateinit var siteListItemBuilder: SiteListItemBuilder

    @Before
    fun setUp() {
        siteListItemBuilder = SiteListItemBuilder(
            accountStore,
            pluginUtilsWrapper,
            siteUtilsWrapper,
            buildConfigWrapper,
            themeBrowserUtils,
            siteDomainsFeatureConfig
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

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(ACTIVITY_ITEM)
    }

    @Test
    fun `activity item built when site is Jetpack, can manage options and is not WP for teams`() {
        setupActivityLogItem(
            isAccessedViaWPComRest = false,
            canManageOptions = true,
            isJetpackConnected = true,
            isWpForTeams = false
        )

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(ACTIVITY_ITEM)
    }

    @Test
    fun `activity item not built when site cannot manage options`() {
        setupActivityLogItem(canManageOptions = false)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Test
    fun `activity item not built when site is WP for teams`() {
        setupActivityLogItem(isWpForTeams = true)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Test
    fun `activity item not built when site is neither Jetpack nor uses WPComRest`() {
        setupActivityLogItem(isAccessedViaWPComRest = false, isJetpackConnected = false)

        val item = siteListItemBuilder.buildActivityLogItemIfAvailable(siteModel, SITE_ITEM_ACTION)

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
    fun `backup item built if backup is available`() {
        val isBackupsAvailable = true

        val item = siteListItemBuilder.buildBackupItemIfAvailable(SITE_ITEM_ACTION, isBackupsAvailable)

        assertThat(item).isEqualTo(BACKUP_ITEM)
    }

    @Test
    fun `backup item not built if backup is not available`() {
        val isBackupsAvailable = false

        val item = siteListItemBuilder.buildBackupItemIfAvailable(SITE_ITEM_ACTION, isBackupsAvailable)

        assertThat(item).isNull()
    }

    @Test
    fun `scan item built if scan is available`() {
        val isScanAvailable = true

        val item = siteListItemBuilder.buildScanItemIfAvailable(SITE_ITEM_ACTION, isScanAvailable)

        assertThat(item).isEqualTo(SCAN_ITEM)
    }

    @Test
    fun `scan item not built if scan is not available`() {
        val isScanAvailable = false

        val item = siteListItemBuilder.buildScanItemIfAvailable(SITE_ITEM_ACTION, isScanAvailable)

        assertThat(item).isNull()
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item built when plan not empty, site can manage options, is not WP for teams and is WPcom`() {
        setupPlanItem(
            planShortName = PLAN_NAME,
            canManageOptions = true,
            isWpForTeams = false,
            isWPCom = true,
            isAutomatedTransfer = false
        )
        val showFocusPoint = true

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, showFocusPoint, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(PLAN_ITEM.copy(showFocusPoint = showFocusPoint))
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item built when plan not empty, site can manage options, is not WP for teams and is AT`() {
        setupPlanItem(
            planShortName = PLAN_NAME,
            canManageOptions = true,
            isWpForTeams = false,
            isWPCom = false,
            isAutomatedTransfer = true
        )

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, false, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(PLAN_ITEM)
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item not built when plan name is empty`() {
        setupPlanItem(planShortName = "")

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, false, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item not built when site cannot manage options`() {
        setupPlanItem(canManageOptions = false)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, false, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item not built when site is WP for teams`() {
        setupPlanItem(isWpForTeams = true)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, false, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Ignore("Ignored after a decision was made to hide the Plans screen.")
    @Test
    fun `plan item not built when site is neither WP com nor AT`() {
        setupPlanItem(isWPCom = false, isAutomatedTransfer = false)

        val item = siteListItemBuilder.buildPlanItemIfAvailable(siteModel, false, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    private fun setupPlanItem(
        planShortName: String = PLAN_NAME,
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
    fun `returns jetpack item when site is jetpack, WPCom, can manage options and is not atomic`() {
        setupJetpackItem(isJetpackConnected = true, isWpCom = true, canManageOptions = true, isAtomic = false)

        val lookAndFeelHeader = siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(lookAndFeelHeader).isEqualTo(JETPACK_ITEM)
    }

    @Test
    fun `does not return jetpack item when site is not jetpack`() {
        setupJetpackItem(isJetpackConnected = false)

        val lookAndFeelHeader = siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `does not return jetpack item when site is not WPCom`() {
        setupJetpackItem(isWpCom = false)

        val lookAndFeelHeader = siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `does not return jetpack item when site can manage options`() {
        setupJetpackItem(canManageOptions = false)

        val lookAndFeelHeader = siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `does not return jetpack item when site is atomic`() {
        setupJetpackItem(isAtomic = true)

        val lookAndFeelHeader = siteListItemBuilder.buildJetpackItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(lookAndFeelHeader).isNull()
    }

    private fun setupJetpackItem(
        isJetpackConnected: Boolean = true,
        isAtomic: Boolean = false,
        isWpCom: Boolean = true,
        canManageOptions: Boolean = true
    ) {
        whenever(siteModel.isJetpackConnected).thenReturn(isJetpackConnected)
        whenever(siteModel.isWPComAtomic).thenReturn(isAtomic)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(isWpCom)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
    }

    @Test
    fun `pages item not built when not self-hosted admin and cannot edit pages`() {
        setupPagesItem(isSelfHostedAdmin = false, canEditPages = false)

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, SITE_ITEM_ACTION, showFocusPoint = false)

        assertThat(item).isNull()
    }

    @Test
    fun `pages item built when self-hosted admin`() {
        setupPagesItem(isSelfHostedAdmin = true)
        val showFocusPoint = true

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, SITE_ITEM_ACTION, showFocusPoint)

        assertThat(item).isEqualTo(PAGES_ITEM.copy(showFocusPoint = showFocusPoint))
    }

    @Test
    fun `pages item built when can edit pages`() {
        setupPagesItem(canEditPages = true)
        val showFocusPoint = true

        val item = siteListItemBuilder.buildPagesItemIfAvailable(siteModel, SITE_ITEM_ACTION, showFocusPoint)

        assertThat(item).isEqualTo(PAGES_ITEM.copy(showFocusPoint = showFocusPoint))
    }

    private fun setupPagesItem(isSelfHostedAdmin: Boolean = false, canEditPages: Boolean = false) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(isSelfHostedAdmin)
        whenever(siteModel.hasCapabilityEditPages).thenReturn(canEditPages)
    }

    @Test
    fun `admin item built when site is not WPCom`() {
        setupAdminItem(isWPCom = false)

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(ADMIN_ITEM)
    }

    @Test
    fun `admin item built when site is WPCom and account created before 2015-10-07`() {
        setupAdminItem(isWPCom = true, accountDate = "2015-10-06T02:00:00+0200")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(ADMIN_ITEM)
    }

    @Test
    fun `admin item built when site is WPCom and account date is empty`() {
        setupAdminItem(isWPCom = true, accountDate = "")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(ADMIN_ITEM)
    }

    @Test
    fun `admin item not built when site is WPCom and account created after 2015-10-07`() {
        setupAdminItem(isWPCom = true, accountDate = "2015-10-08T02:00:00+0200")

        val item = siteListItemBuilder.buildAdminItemIfAvailable(siteModel, SITE_ITEM_ACTION)

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

        val item = siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(PEOPLE_ITEM)
    }

    @Test
    fun `people item not built if site cannot list users`() {
        whenever(siteModel.hasCapabilityListUsers).thenReturn(false)

        val item = siteListItemBuilder.buildPeopleItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Test
    fun `plugin item built if feature available`() {
        whenever(pluginUtilsWrapper.isPluginFeatureAvailable(siteModel)).thenReturn(true)

        val item = siteListItemBuilder.buildPluginItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(PLUGINS_ITEM)
    }

    @Test
    fun `plugin item not built if feature not available`() {
        whenever(pluginUtilsWrapper.isPluginFeatureAvailable(siteModel)).thenReturn(false)

        val item = siteListItemBuilder.buildPluginItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Test
    fun `share item built if site supports sharing`() {
        whenever(siteModel.supportsSharing()).thenReturn(true)

        val item = siteListItemBuilder.buildShareItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(SHARING_ITEM)
    }

    @Test
    fun `share item not built if site does not supports sharing`() {
        whenever(siteModel.supportsSharing()).thenReturn(false)

        val item = siteListItemBuilder.buildShareItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    @Test
    fun `settings item built if site can manage options`() {
        setupSiteSettings(canManageOptions = true)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(SITE_SETTINGS_ITEM)
    }

    @Test
    fun `settings item built if site is not accessed through WPCom REST`() {
        setupSiteSettings(isAccessedViaWPComRest = false)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isEqualTo(SITE_SETTINGS_ITEM)
    }

    @Test
    fun `settings item not built if site is accessed through WPCom REST and cannot manage options`() {
        setupSiteSettings(canManageOptions = false, isAccessedViaWPComRest = true)

        val item = siteListItemBuilder.buildSiteSettingsItemIfAvailable(siteModel, SITE_ITEM_ACTION)

        assertThat(item).isNull()
    }

    private fun setupSiteSettings(canManageOptions: Boolean = false, isAccessedViaWPComRest: Boolean = true) {
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(isAccessedViaWPComRest)
    }
}
