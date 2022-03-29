package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.tabs.MySiteDefaultTabExperiment
import org.wordpress.android.ui.mysite.tabs.MySiteTabExperimentVariant
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentFeatureConfig
import org.wordpress.android.util.config.MySiteDefaultTabExperimentVariationDashboardFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class MySiteDefaultTabExperimentTest : BaseUnitTest() {
    @Mock lateinit var mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig
    @Mock lateinit var mySiteDefaultTabExperimentFeatureConfig: MySiteDefaultTabExperimentFeatureConfig
    @Mock lateinit var mySiteDefaultTabExperimentVariationDashboardFeatureConfig:
            MySiteDefaultTabExperimentVariationDashboardFeatureConfig
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var mySiteDefaultTabExperiment: MySiteDefaultTabExperiment

    @Before
    fun setUp() {
        init()
    }

    fun init() {
        mySiteDefaultTabExperiment = MySiteDefaultTabExperiment(
                mySiteDefaultTabExperimentFeatureConfig,
                mySiteDefaultTabExperimentVariationDashboardFeatureConfig,
                mySiteDashboardTabsFeatureConfig,
                appPrefsWrapper,
                analyticsTrackerWrapper
        )
    }

    @Test
    fun `given my site tabs feature flag is enabled, when check and set variant, then app prefs is not set`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariant(any())
    }

    @Test
    fun `given default tab experiment flag is not enabled, when check and set variant, then app prefs is not set`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariant(any())
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then app prefs is set`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.getMySiteDefaultTabExperimentVariant())
                .thenReturn(MySiteTabExperimentVariant.NONEXISTENT.label)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, atLeastOnce()).setMySiteDefaultTabExperimentVariant(any())
    }

    @Test
    fun `given experiment is running, when variant is already set, then app prefs is not reset`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.getMySiteDefaultTabExperimentVariant())
                .thenReturn(MySiteTabExperimentVariant.DASHBOARD.label)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariant(any())
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then experiment inject properties are set`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.getMySiteDefaultTabExperimentVariant())
                .thenReturn(MySiteTabExperimentVariant.NONEXISTENT.label)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, atLeastOnce()).setInjectExperimentProperties(any())
    }

    @Test
    fun `given experiment is running, when variant is already set, then experiment inject properties are not set`() {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.getMySiteDefaultTabExperimentVariant())
                .thenReturn(MySiteTabExperimentVariant.DASHBOARD.label)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, never()).setInjectExperimentProperties(any())
    }
}
