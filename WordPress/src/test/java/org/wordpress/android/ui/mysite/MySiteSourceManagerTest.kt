package org.wordpress.android.ui.mysite

import androidx.lifecycle.MediatorLiveData
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.cards.dashboard.CardsSource
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig

/* SITE */

const val SITE_LOCAL_ID = 1

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MySiteSourceManagerTest : BaseUnitTest() {
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var domainRegistrationSource: DomainRegistrationSource
    @Mock lateinit var scanAndBackupSource: ScanAndBackupSource
    @Mock lateinit var currentAvatarSource: CurrentAvatarSource
    @Mock lateinit var dynamicCardsSource: DynamicCardsSource
    @Mock lateinit var cardsSource: CardsSource
    @Mock lateinit var quickStartCardSource: QuickStartCardSource
    @Mock lateinit var siteIconProgressSource: SiteIconProgressSource
    @Mock lateinit var selectedSiteSource: SelectedSiteSource
    @Mock lateinit var mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
    private lateinit var mySiteSourceManager: MySiteSourceManager
    private val selectedSite = MediatorLiveData<SelectedSite>()
    private lateinit var allRefreshedMySiteSources: List<MySiteSource<*>>
    private lateinit var allRefreshedMySiteSourcesExceptCardsSource: List<MySiteSource<*>>
    private lateinit var siteIndependentMySiteSources: List<MySiteSource<*>>
    private lateinit var selectRefreshedMySiteSources: List<MySiteSource<*>>

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        selectedSite.value = null
        mySiteSourceManager = MySiteSourceManager(
                analyticsTrackerWrapper,
                currentAvatarSource,
                domainRegistrationSource,
                dynamicCardsSource,
                quickStartCardSource,
                scanAndBackupSource,
                selectedSiteSource,
                cardsSource,
                siteIconProgressSource,
                mySiteDashboardPhase2FeatureConfig
        )

        allRefreshedMySiteSources = listOf(
                selectedSiteSource,
                siteIconProgressSource,
                quickStartCardSource,
                currentAvatarSource,
                domainRegistrationSource,
                scanAndBackupSource,
                dynamicCardsSource,
                cardsSource
        )

        allRefreshedMySiteSourcesExceptCardsSource = listOf(
                selectedSiteSource,
                siteIconProgressSource,
                quickStartCardSource,
                currentAvatarSource,
                domainRegistrationSource,
                scanAndBackupSource,
                dynamicCardsSource
        )

        siteIndependentMySiteSources = listOf(
                currentAvatarSource
        )

        selectRefreshedMySiteSources = listOf(
                quickStartCardSource,
                currentAvatarSource
        )
    }

    /* ON REFRESH */

    @Test
    fun `given with side local id and phase 2 enabled, when build, then all sources are built`() {
        val coroutineScope = testScope()
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)

        mySiteSourceManager.build(coroutineScope, SITE_LOCAL_ID)

        allRefreshedMySiteSources.forEach { verify(it).build(coroutineScope, SITE_LOCAL_ID) }
    }

    @Test
    fun `given with side local id and phase 2 disabled, when build, then all sources except cards source are built`() {
        val coroutineScope = testScope()
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.build(coroutineScope, SITE_LOCAL_ID)

        allRefreshedMySiteSourcesExceptCardsSource.forEach { verify(it).build(coroutineScope, SITE_LOCAL_ID) }
        verify(cardsSource, times(0)).build(coroutineScope, SITE_LOCAL_ID)
    }

    @Test
    fun `given without site local id, when build, then all sources except cards source are built`() {
        val coroutineScope = testScope()

        mySiteSourceManager.build(coroutineScope, null)

        siteIndependentMySiteSources.forEach { verify(it as SiteIndependentSource).build(coroutineScope) }
    }

    /* ON REFRESH */

    @Test
    fun `given phase 2 is enabled, when refresh, then all sources are refreshed`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)

        mySiteSourceManager.refresh()

        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach { verify(it).refresh() }
    }

    @Test
    fun `given phase 2 is disabled, when refresh, then select sources refresh`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.refresh()

        selectRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach { verify(it).refresh() }
    }

    @Test
    fun `given phase 2 disabled, when refresh, then updateSiteSettings is invoked`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.refresh()

        verify(selectedSiteSource).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `given phase 2 is enabled, when refreshing, then isRefreshing should return true`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            whenever(it.isRefreshing()).thenReturn(true)
        }

        val result = mySiteSourceManager.isRefreshing()

        assertThat(result).isTrue
    }

    @Test
    fun `given phase 2 enabled, when is not refreshing, then isRefreshing should return false`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            whenever(it.isRefreshing()).thenReturn(false)
        }

        val result = mySiteSourceManager.isRefreshing()

        assertThat(result).isFalse
    }

    @Test
    fun `given phase 2 is disabled, when is refreshing, then isRefreshing should return false`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        val result = mySiteSourceManager.isRefreshing()

        assertThat(result).isFalse
    }

    /* ON RESUME */

    @Test
    fun `given not first resume and phase 2 disabled, when on resume, then update site settings if necessary`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.onResume(false)

        verify(selectedSiteSource).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `given not first resume and phase 2 disabled, when on resume, then refresh quick start`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.onResume(false)

        verify(quickStartCardSource).refresh()
    }

    @Test
    fun `given not first resume and phase 2 disabled, when on resume, then refresh current avatar`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)

        mySiteSourceManager.onResume(false)

        verify(currentAvatarSource).refresh()
    }

    @Test
    fun `given first resume and phase 2 disabled, when on resume, then update site settings if necessary`() {
        mySiteSourceManager.onResume(true)

        verify(selectedSiteSource).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `given first resume and phase 2 disabled, when on resume, then refresh quick start`() {
        mySiteSourceManager.onResume(true)

        verify(quickStartCardSource).refresh()
    }

    @Test
    fun `given first resume and phase 2 disabled, when on resume, then refresh current avatar`() {
        mySiteSourceManager.onResume(true)

        verify(currentAvatarSource).refresh()
    }

    @Test
    fun `given first resume and phase 2 enabled, when on resume, then update site settings if necessary`() {
        mySiteSourceManager.onResume(true)

        verify(selectedSiteSource).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `given first resume and phase 2 enabled, when on resume, then refresh quick start`() {
        mySiteSourceManager.onResume(true)

        verify(quickStartCardSource).refresh()
    }

    @Test
    fun `given first resume and phase 2 enabled, when on resume, then refresh current avatar`() {
        mySiteSourceManager.onResume(true)

        verify(currentAvatarSource).refresh()
    }

    @Test
    fun `given first resume and phase 2 enabled, when on resume, then refresh is invoked`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)

        mySiteSourceManager.onResume(false)

        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach { verify(it).refresh() }
    }

    /* ON CLEAR */

    @Test
    fun `when clear is invoked, then domainRegistrationSource clear() is invoked`() {
        mySiteSourceManager.clear()

        verify(domainRegistrationSource).clear()
    }

    @Test
    fun `when clear is invoked, then scanAndBackupSource clear() is invoked`() {
        mySiteSourceManager.clear()

        verify(scanAndBackupSource).clear()
    }

    @Test
    fun `when clear is invoked, then selectedSiteSource clear() is invoked`() {
        mySiteSourceManager.clear()

        verify(selectedSiteSource).clear()
    }

    /* DYNAMIC CARDS HIDE/REMOVE */

    @Test
    fun `when dynamic QS hide menu item is clicked, then the card is hidden`() = test {
        val id = DynamicCardType.CUSTOMIZE_QUICK_START
        mySiteSourceManager.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Hide(id))

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_HIDE_CARD_TAPPED)
        verify(dynamicCardsSource).hideItem(id)
    }

    @Test
    fun `when dynamic QS remove menu item is clicked, then the card is removed`() = test {
        val id = DynamicCardType.CUSTOMIZE_QUICK_START
        mySiteSourceManager.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Remove(id))

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_REMOVE_CARD_TAPPED)
        verify(dynamicCardsSource).removeItem(id)
    }

    /* QUICK START */

    @Test
    fun `when quick start is refreshed, then quickStartCardSource refresh() is invoked`() {
        mySiteSourceManager.refreshQuickStart()

        verify(quickStartCardSource).refresh()
    }
}
