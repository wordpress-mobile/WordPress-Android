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
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.tabs.MySiteDefaultTabExperiment
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
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

    fun init(
        isMySiteDashboardTabsFeatureConfigEnabled: Boolean = true,
        isMySiteDefaultTabExperimentFeatureConfigEnabled: Boolean = false
    ) {
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(isMySiteDashboardTabsFeatureConfigEnabled)
        whenever(mySiteDefaultTabExperimentFeatureConfig.isEnabled())
                .thenReturn(isMySiteDefaultTabExperimentFeatureConfigEnabled)
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
        init(isMySiteDashboardTabsFeatureConfigEnabled = true)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariantAssigned()
    }

    @Test
    fun `given default tab experiment flag is not enabled, when check and set variant, then app prefs is not set`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = false
        )

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariantAssigned()
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then assigned prefs is set`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, atLeastOnce()).setMySiteDefaultTabExperimentVariantAssigned()
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then initial screen prefs is set`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(false)
        whenever(mySiteDefaultTabExperimentVariationDashboardFeatureConfig.isDashboardVariant()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(
                appPrefsWrapper,
                atLeastOnce()
        ).setInitialScreenFromMySiteDefaultTabExperimentVariant(MySiteTabType.SITE_MENU.trackingLabel)
    }

    @Test
    fun `given experiment is running, when variant is already set, then app prefs is not reset`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(true)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(appPrefsWrapper, never()).setMySiteDefaultTabExperimentVariantAssigned()
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then experiment inject properties are set`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, atLeastOnce()).setInjectExperimentProperties(any())
    }

    @Test
    fun `given experiment is running, when variant is already set, then experiment inject properties are not set`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(true)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, never()).setInjectExperimentProperties(any())
    }

    @Test
    fun `given experiment is running, when variant is not assigned, then assignment is tracked`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(false)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, atLeastOnce()).track(Stat.MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_ASSIGNED)
    }

    @Test
    fun `given experiment is running, when variant is assigned, then assignment is not tracked`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(true)

        mySiteDefaultTabExperiment.checkAndSetVariantIfNeeded()

        verify(analyticsTrackerWrapper, never()).track(Stat.MY_SITE_DEFAULT_TAB_EXPERIMENT_VARIANT_ASSIGNED)
    }

    @Test
    fun `given experiment is running and unassigned, when request for reassign, then variant is not reassigned`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(false)

        mySiteDefaultTabExperiment.changeExperimentVariantAssignmentIfNeeded(MySiteTabType.SITE_MENU.trackingLabel)

        verify(
                appPrefsWrapper,
                never()
        ).setInitialScreenFromMySiteDefaultTabExperimentVariant(MySiteTabType.SITE_MENU.trackingLabel)
    }

    @Test
    fun `given experiment is running and assigned, when request for reassign, then variant is reassigned`() {
        init(
                isMySiteDashboardTabsFeatureConfigEnabled = true,
                isMySiteDefaultTabExperimentFeatureConfigEnabled = true
        )
        whenever(appPrefsWrapper.isMySiteDefaultTabExperimentVariantAssigned()).thenReturn(true)

        mySiteDefaultTabExperiment.changeExperimentVariantAssignmentIfNeeded(MySiteTabType.SITE_MENU.trackingLabel)

        verify(
                appPrefsWrapper,
                atLeastOnce()
        ).setInitialScreenFromMySiteDefaultTabExperimentVariant(MySiteTabType.SITE_MENU.trackingLabel)
    }
}
