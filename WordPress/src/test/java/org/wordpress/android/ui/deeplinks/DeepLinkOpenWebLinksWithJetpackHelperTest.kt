package org.wordpress.android.ui.deeplinks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.FirebaseRemoteConfigWrapper
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.config.OpenWebLinksWithJetpackFlowFeatureConfig
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class DeepLinkOpenWebLinksWithJetpackHelperTest : BaseUnitTest() {
    @Mock lateinit var openWebLinksWithJetpackFlowFeatureConfig: OpenWebLinksWithJetpackFlowFeatureConfig
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var firebaseRemoteConfigWrapper: FirebaseRemoteConfigWrapper
    @Mock lateinit var packageManagerWrapper: PackageManagerWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper

    private lateinit var helper: DeepLinkOpenWebLinksWithJetpackHelper

    @Before
    fun setUp() {
        helper = DeepLinkOpenWebLinksWithJetpackHelper(
                openWebLinksWithJetpackFlowFeatureConfig,
                appPrefsWrapper,
                firebaseRemoteConfigWrapper,
                packageManagerWrapper,
                dateTimeUtilsWrapper,
                buildConfigWrapper
        )
    }

    @Test
    fun `when feature flag is off, then show overlay is false`() {
        setTest(isFeatureFlagEnabled = false)

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow enabled, when JP is not installed, then show overlay is false`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = false
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow enabled & JP is installed, when isOpenWebLinks setting is enabled, then show overlay is false`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = true
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isFalse
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given flow enabled, JP install, isOpenWebLinks disabled, when overlay never been shown, then show overlay is true`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = false,
                overlayLastShownTimestamp = 0L
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isTrue
    }

    @Test
    @Suppress("MaxLineLength")
    fun `given flow enabled, JP install, isOpenWebLinks disabled, overlay shown before, when frequency is only once, then show overlay is false`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = false,
                overlayLastShownTimestamp = getDateXDaysAgoInMilliseconds(5),
                flowFrequency = 0L
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow is accessed after frequency, when showOverlay is invoked, then show overlay is true`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = false,
                overlayLastShownTimestamp = getDateXDaysAgoInMilliseconds(9),
                flowFrequency = 5L
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isTrue
    }

    @Test
    fun `given flow is accessed before frequency, when showOverlay is invoked, then show overlay is false`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = false,
                overlayLastShownTimestamp = getDateXDaysAgoInMilliseconds(3),
                flowFrequency = 5L
        )
        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow is accessed equal to frequency, when showOverlay is invoked, then show overlay is true`() {
        setTest(
                isFeatureFlagEnabled = true,
                isJetpackInstalled = true,
                isOpenWebLinksWithJetpack = false,
                overlayLastShownTimestamp = getDateXDaysAgoInMilliseconds(1),
                flowFrequency = 1L
        )

        val result = helper.shouldShowDeepLinkOpenWebLinksWithJetpackOverlay()

        assertThat(result).isTrue
    }

    @Test
    fun `when feature flag is off, then show app setting is false`() {
        setTest(isFeatureFlagEnabled = false)

        val result = helper.shouldShowAppSetting()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow ff is enabled, when jetpack is not installed, then show app setting is false`() {
        setTest(isFeatureFlagEnabled = true,
            isJetpackInstalled = false)

        val result = helper.shouldShowAppSetting()

        assertThat(result).isFalse
    }

    @Test
    fun `given flow ff is enabled, when jetpack is installed, then show app setting is true`() {
        setTest(isFeatureFlagEnabled = true,
                isJetpackInstalled = true)

        val result = helper.shouldShowAppSetting()

        assertThat(result).isTrue
    }

    private fun setTest(
        isFeatureFlagEnabled: Boolean = true,
        isJetpackInstalled: Boolean = true,
        isOpenWebLinksWithJetpack: Boolean = false,
        overlayLastShownTimestamp: Long = 0L,
        flowFrequency: Long = 0L
    ) {
        setFeatureEnabled(isFeatureFlagEnabled)
        setJetpackInstalled(isJetpackInstalled)
        setIsOpenWebLinksWithJetpack(isOpenWebLinksWithJetpack)
//        setDeepLinkHandlerComponentEnabled(isDeepLinkComponentEnabled)
        setLastShownTimestamp(overlayLastShownTimestamp)
        setFlowFrequency(flowFrequency)
        setDaysBetween(overlayLastShownTimestamp)
        setPackageName(PACKAGE_NAME)
    }

    // Helpers
    private fun setFeatureEnabled(value: Boolean) {
        whenever(openWebLinksWithJetpackFlowFeatureConfig.isEnabled()).thenReturn(value)
    }

    private fun setJetpackInstalled(value: Boolean) {
        whenever(packageManagerWrapper.isPackageInstalled(anyString())).thenReturn(value)
    }

    private fun setLastShownTimestamp(value: Long) {
        whenever(appPrefsWrapper.getOpenWebLinksWithJetpackOverlayLastShownTimestamp()).thenReturn(value)
    }

    private fun setFlowFrequency(value: Long) {
        whenever(firebaseRemoteConfigWrapper.getOpenWebLinksWithJetpackFlowFrequency()).thenReturn(value)
    }

    private fun setDaysBetween(lastShownTimestamp: Long) {
        val between =
                DateTimeUtils.daysBetween(Date(lastShownTimestamp), Date(getDateXDaysAgoInMilliseconds(0)))

        whenever(dateTimeUtilsWrapper.daysBetween(any(), any())).thenReturn(between)
    }

    private fun setIsOpenWebLinksWithJetpack(value: Boolean) {
        whenever(appPrefsWrapper.getIsOpenWebLinksWithJetpack()).thenReturn(value)
    }

    private fun setPackageName(value: String) {
        whenever(buildConfigWrapper.getApplicationId()).thenReturn(value)
    }

    private fun getDateXDaysAgoInMilliseconds(daysAgo: Int) =
            System.currentTimeMillis().minus(DAY_IN_MILLISECONDS * daysAgo)

    companion object {
        private const val DAY_IN_MILLISECONDS = 86400000
        private const val PACKAGE_NAME = "com.jetpack.android"
    }
}
