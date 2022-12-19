package org.wordpress.android.ui.mysite

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.cards.dashboard.CardsSource
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardSource
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.ui.quickstart.QuickStartTracker

/* SITE */

const val SITE_LOCAL_ID = 1

@RunWith(MockitoJUnitRunner::class)
class MySiteSourceManagerTest : BaseUnitTest() {
    @Mock lateinit var quickStartTracker: QuickStartTracker
    @Mock lateinit var domainRegistrationSource: DomainRegistrationSource
    @Mock lateinit var scanAndBackupSource: ScanAndBackupSource
    @Mock lateinit var currentAvatarSource: CurrentAvatarSource
    @Mock lateinit var dynamicCardsSource: DynamicCardsSource
    @Mock lateinit var cardsSource: CardsSource
    @Mock lateinit var quickStartCardSource: QuickStartCardSource
    @Mock lateinit var siteIconProgressSource: SiteIconProgressSource
    @Mock lateinit var selectedSiteSource: SelectedSiteSource
    @Mock lateinit var bloggingPromptCardSource: BloggingPromptCardSource
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var siteModel: SiteModel
    private lateinit var mySiteSourceManager: MySiteSourceManager
    private val selectedSite = MediatorLiveData<SelectedSite>()
    private lateinit var allRefreshedMySiteSources: List<MySiteSource<*>>
    private lateinit var allRefreshedMySiteSourcesExceptCardsSource: List<MySiteSource<*>>
    private lateinit var siteIndependentMySiteSources: List<MySiteSource<*>>
    private lateinit var selectRefreshedMySiteSources: List<MySiteSource<*>>
    private lateinit var siteDependentMySiteSources: List<MySiteSource<*>>

    @InternalCoroutinesApi
    @Before
    fun setUp() = test {
        selectedSite.value = null
        whenever(siteModel.isUsingWpComRestApi).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(selectedSiteRepository.hasSelectedSite()).thenReturn(true)

        mySiteSourceManager = MySiteSourceManager(
                quickStartTracker,
                currentAvatarSource,
                domainRegistrationSource,
                dynamicCardsSource,
                quickStartCardSource,
                scanAndBackupSource,
                selectedSiteSource,
                cardsSource,
                siteIconProgressSource,
                bloggingPromptCardSource,
                selectedSiteRepository
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

        siteDependentMySiteSources = allRefreshedMySiteSources.filterNot(SiteIndependentSource::class.java::isInstance)
    }

    /* ON REFRESH */

    @Test
    fun `given with site local id, when build, then all sources are built`() {
        val coroutineScope = testScope()

        mySiteSourceManager.build(coroutineScope, SITE_LOCAL_ID)

        allRefreshedMySiteSources.forEach { verify(it).build(coroutineScope, SITE_LOCAL_ID) }
    }

    @Test
    fun `given without site local id, when build, then all site independent sources are built`() {
        val coroutineScope = testScope()

        mySiteSourceManager.build(coroutineScope, null)

        siteIndependentMySiteSources.forEach { verify(it as SiteIndependentSource).build(coroutineScope) }
    }

    @Test
    fun `given without site local id, when build, then site dependent sources are not built`() {
        val coroutineScope = testScope()

        mySiteSourceManager.build(coroutineScope, null)

        siteDependentMySiteSources.forEach { verify(it, times(0)).build(coroutineScope, SITE_LOCAL_ID) }
    }

    @Test
    fun `given without site local id, when refresh, then site independent sources are built`() {
        val coroutineScope = testScope()
        mySiteSourceManager.build(coroutineScope, null)

        mySiteSourceManager.refresh()

        siteIndependentMySiteSources.forEach { verify(it as SiteIndependentSource).build(coroutineScope) }
    }

    @Test
    fun `given without site local id, when refresh, then site dependent sources are not built`() {
        val coroutineScope = testScope()
        mySiteSourceManager.build(coroutineScope, null)

        mySiteSourceManager.refresh()

        siteDependentMySiteSources.forEach { verify(it, times(0)).build(coroutineScope, SITE_LOCAL_ID) }
    }

    @Test
    fun `given non wpcom site, when build, then all sources except cards source are built`() {
        val coroutineScope = testScope()
        whenever(siteModel.isUsingWpComRestApi).thenReturn(false)

        mySiteSourceManager.build(coroutineScope, SITE_LOCAL_ID)

        allRefreshedMySiteSourcesExceptCardsSource.forEach { verify(it).build(coroutineScope, SITE_LOCAL_ID) }
        verify(cardsSource, times(0)).build(coroutineScope, SITE_LOCAL_ID)
    }

    /* ON REFRESH */

    @Test
    fun `when refresh, then all sources are refreshed`() {
        mySiteSourceManager.refresh()

        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach { verify(it).refresh() }
    }

    @Test
    fun `when refreshing, then isRefreshing should return true`() {
        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            whenever(it.isRefreshing()).thenReturn(true)
        }

        val result = mySiteSourceManager.isRefreshing()

        assertThat(result).isTrue
    }

    @Test
    fun `when is not refreshing, then isRefreshing should return false`() {
        allRefreshedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            whenever(it.isRefreshing()).thenReturn(false)
        }

        val result = mySiteSourceManager.isRefreshing()

        assertThat(result).isFalse
    }

    /* ON RESUME */

    @Test
    fun `given site selected, when on resume, then update site settings if necessary`() {
        mySiteSourceManager.onResume(true)

        verify(selectedSiteSource).updateSiteSettingsIfNecessary()
    }

    @Test
    fun `given site selected, when on resume, then refresh quick start`() {
        mySiteSourceManager.onResume(true)

        verify(quickStartCardSource).refresh()
    }

    @Test
    fun `given site selected, when on resume, then refresh current avatar`() {
        mySiteSourceManager.onResume(true)

        verify(currentAvatarSource).refresh()
    }

    @Test
    fun `given site selected, when on resume, then refresh is invoked`() {
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

        verify(quickStartTracker).track(Stat.QUICK_START_HIDE_CARD_TAPPED)
        verify(dynamicCardsSource).hideItem(id)
    }

    @Test
    fun `when dynamic QS remove menu item is clicked, then the card is removed`() = test {
        val id = DynamicCardType.CUSTOMIZE_QUICK_START
        mySiteSourceManager.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Remove(id))

        verify(quickStartTracker).track(Stat.QUICK_START_REMOVE_CARD_TAPPED)
        verify(dynamicCardsSource).removeItem(id)
    }

    /* QUICK START */

    @Test
    fun `when quick start is refreshed, then quickStartCardSource refresh() is invoked`() {
        mySiteSourceManager.refreshQuickStart()

        verify(quickStartCardSource).refresh()
    }

    /* BLOGGING PROMPTS */

    @Test
    fun `refreshing blogging single blogging prompt calls refreshTodayPrompt() method of BP card source`() {
        mySiteSourceManager.refreshBloggingPrompts(true)

        verify(bloggingPromptCardSource).refreshTodayPrompt()
    }

    @Test
    fun `refreshing all blogging prompts single blogging prompt calls refresh() method of BP card source`() {
        mySiteSourceManager.refreshBloggingPrompts(false)

        verify(bloggingPromptCardSource).refresh()
    }
}
