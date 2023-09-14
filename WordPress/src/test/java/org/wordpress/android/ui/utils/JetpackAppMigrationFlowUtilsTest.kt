package org.wordpress.android.ui.utils

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackMigrationFlowFeatureConfig
import org.wordpress.android.util.publicdata.AppStatus
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider

@RunWith(MockitoJUnitRunner::class)
class JetpackAppMigrationFlowUtilsTest {
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val jetpackMigrationFlowFeatureConfig: JetpackMigrationFlowFeatureConfig = mock()
    private val contextProvider: ContextProvider = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val accountStore: AccountStore = mock()
    private val appStatus: AppStatus = mock()
    private val wordPressPublicData: WordPressPublicData = mock()
    private val contentMigrationAnalyticsTracker: ContentMigrationAnalyticsTracker = mock()

    private val jetpackAppMigrationFlowUtils = JetpackAppMigrationFlowUtils(
        buildConfigWrapper,
        jetpackMigrationFlowFeatureConfig,
        contextProvider,
        appPrefsWrapper,
        accountStore,
        appStatus,
        wordPressPublicData,
        contentMigrationAnalyticsTracker,
    )

    @Before
    fun setUp() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(jetpackMigrationFlowFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationEligible()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("package")
        whenever(appStatus.isAppInstalled(any())).thenReturn(true)
        whenever(wordPressPublicData.nonSemanticPackageVersion()).thenReturn("21.3")
    }

    @Test
    fun `When all conditions are met the migration flow should be shown`() {
        val expected = true
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When not in the Jetpack app the migration flow should not be shown`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When the jetpackMigrationFlow is not enabled the Jetpack app the migration flow should not be shown`() {
        whenever(jetpackMigrationFlowFeatureConfig.isEnabled()).thenReturn(false)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When the jetpackMigrationFlow is not eligible the Jetpack app the migration flow should not be shown`() {
        whenever(appPrefsWrapper.isJetpackMigrationEligible()).thenReturn(false)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When user is logged in and migration was in progress the Jetpack app the migration flow should be shown`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationInProgress()).thenReturn(true)
        val expected = true
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `If the user is logged in the Jetpack app the migration flow should not be shown`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When the WordPress app is not installed the Jetpack app the migration flow should not be shown`() {
        whenever(appStatus.isAppInstalled(any())).thenReturn(false)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When the WordPress app is not compatible the Jetpack app the migration flow should not be shown`() {
        whenever(wordPressPublicData.nonSemanticPackageVersion()).thenReturn("21.2")
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
        verify(contentMigrationAnalyticsTracker).trackWordPressAppDetected(false)
    }

    @Test
    fun `When the WordPress app version is null the Jetpack app the migration flow should not be shown`() {
        whenever(wordPressPublicData.nonSemanticPackageVersion()).thenReturn(null)
        val expected = false
        val actual = jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `When the WordPress app is installed and compatible detection should be tracked true`() {
        jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()

        verify(contentMigrationAnalyticsTracker).trackWordPressAppDetected(true)
    }

    @Test
    fun `When the WordPress app is installed and not compatible detection should be tracked as false`() {
        whenever(wordPressPublicData.nonSemanticPackageVersion()).thenReturn("21.2")
        jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()

        verify(contentMigrationAnalyticsTracker).trackWordPressAppDetected(false)
    }

    @Test
    fun `When the WordPress app is not detected detection should not be tracked`() {
        whenever(appStatus.isAppInstalled(any())).thenReturn(false)
        jetpackAppMigrationFlowUtils.shouldShowMigrationFlow()

        verify(contentMigrationAnalyticsTracker, never()).trackWordPressAppDetected(any())
    }
}
