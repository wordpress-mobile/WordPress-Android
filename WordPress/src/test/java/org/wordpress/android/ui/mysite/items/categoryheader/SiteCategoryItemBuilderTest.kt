package org.wordpress.android.ui.mysite.items.categoryheader

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.SiteUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class SiteCategoryItemBuilderTest {
    @Mock lateinit var themeBrowserUtils: ThemeBrowserUtils
    @Mock lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Mock lateinit var siteModel: SiteModel
    private lateinit var siteCategoryItemBuilder: SiteCategoryItemBuilder

    @Before
    fun setUp() {
        siteCategoryItemBuilder = SiteCategoryItemBuilder(themeBrowserUtils, siteUtilsWrapper)
    }

    @Test
    fun `returns jetpack header when site is jetpack, WPCom, can manage options and is not atomic`() {
        setupJetpackHeader(isJetpackConnected = true, isAtomic = false)

        val lookAndFeelHeader = siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)

        assertThat(lookAndFeelHeader).isEqualTo(CategoryHeaderItem(UiStringRes(R.string.my_site_header_jetpack)))
    }

    @Test
    fun `does not return jetpack header when site is not jetpack`() {
        setupJetpackHeader(isJetpackConnected = false)

        val lookAndFeelHeader = siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `does not return jetpack header when site is atomic`() {
        setupJetpackHeader(isAtomic = true)

        val lookAndFeelHeader = siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `returns look and feel header when themes accessible`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(true)

        val lookAndFeelHeader = siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(siteModel)

        assertThat(lookAndFeelHeader).isEqualTo(CategoryHeaderItem(UiStringRes(R.string.my_site_header_look_and_feel)))
    }

    @Test
    fun `does not return look and feel header when themes not accessible`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)

        val lookAndFeelHeader = siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(siteModel)

        assertThat(lookAndFeelHeader).isNull()
    }

    @Test
    fun `returns configuration header when site is not WPCom`() {
        setupConfigurationHeader(isWpCom = false)

        val configurationHeader = siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)

        assertThat(configurationHeader)
                .isEqualTo(CategoryHeaderItem(UiStringRes(R.string.my_site_header_configuration)))
    }

    @Test
    fun `returns configuration header when site can list users`() {
        setupConfigurationHeader(canListUsers = true)

        val configurationHeader = siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)

        assertThat(configurationHeader)
                .isEqualTo(CategoryHeaderItem(UiStringRes(R.string.my_site_header_configuration)))
    }

    @Test
    fun `returns configuration header when site can manage options`() {
        setupConfigurationHeader(canManageOptions = true)

        val configurationHeader = siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)

        assertThat(configurationHeader)
                .isEqualTo(CategoryHeaderItem(UiStringRes(R.string.my_site_header_configuration)))
    }

    @Test
    fun `does not return configuration header when site cannot manage options, list users and is WPCom`() {
        setupConfigurationHeader(canManageOptions = false, isWpCom = true, canListUsers = false)

        val configurationHeader = siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)

        assertThat(configurationHeader).isNull()
    }

    private fun setupJetpackHeader(
        isJetpackConnected: Boolean = true,
        isAtomic: Boolean = false
    ) {
        whenever(siteModel.isJetpackConnected).thenReturn(isJetpackConnected)
        whenever(siteModel.isWPComAtomic).thenReturn(isAtomic)
    }

    private fun setupConfigurationHeader(
        canManageOptions: Boolean = false,
        isWpCom: Boolean = true,
        canListUsers: Boolean = false
    ) {
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(canManageOptions)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(isWpCom)
        whenever(siteModel.hasCapabilityListUsers).thenReturn(canListUsers)
    }
}
