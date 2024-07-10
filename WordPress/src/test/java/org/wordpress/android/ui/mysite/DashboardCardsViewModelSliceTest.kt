package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.atMost
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.bloganuary.BloganuaryNudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.migration.JpMigrationSuccessCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.nocards.NoCardsMessageViewModelSlice
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.plans.PlansCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.quicklinksitem.QuickLinksItemViewModelSlice
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewModelSlice

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DashboardCardsViewModelSliceTest: BaseUnitTest() {
    @Mock
    lateinit var jpMigrationSuccessCardViewModelSlice: JpMigrationSuccessCardViewModelSlice
    @Mock
    lateinit var jetpackInstallFullPluginCardViewModelSlice: JetpackInstallFullPluginCardViewModelSlice
    @Mock
    lateinit var domainRegistrationCardViewModelSlice: DomainRegistrationCardViewModelSlice
    @Mock
    lateinit var blazeCardViewModelSlice: BlazeCardViewModelSlice
    @Mock
    lateinit var cardViewModelSlice: CardViewModelSlice
    @Mock
    lateinit var personalizeCardViewModelSlice: PersonalizeCardViewModelSlice
    @Mock
    lateinit var bloggingPromptCardViewModelSlice: BloggingPromptCardViewModelSlice
    @Mock
    lateinit var quickStartCardViewModelSlice: QuickStartCardViewModelSlice
    @Mock
    lateinit var noCardsMessageViewModelSlice: NoCardsMessageViewModelSlice
    @Mock
    lateinit var quickLinksItemViewModelSlice: QuickLinksItemViewModelSlice
    @Mock
    lateinit var bloganuaryNudgeCardViewModelSlice: BloganuaryNudgeCardViewModelSlice
    @Mock
    lateinit var plansCardViewModelSlice: PlansCardViewModelSlice
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var dashboardCardsViewModelSlice: DashboardCardsViewModelSlice

    @Before
    fun setup() {
        whenever(jpMigrationSuccessCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(jetpackInstallFullPluginCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(domainRegistrationCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(blazeCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(cardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(personalizeCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(bloggingPromptCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(quickStartCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(quickLinksItemViewModelSlice.uiState).thenReturn(MutableLiveData())
        whenever(bloganuaryNudgeCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(plansCardViewModelSlice.uiModel).thenReturn(MutableLiveData())

        dashboardCardsViewModelSlice = DashboardCardsViewModelSlice(
            testDispatcher(),
            jpMigrationSuccessCardViewModelSlice,
            jetpackInstallFullPluginCardViewModelSlice,
            domainRegistrationCardViewModelSlice,
            blazeCardViewModelSlice,
            cardViewModelSlice,
            personalizeCardViewModelSlice,
            bloggingPromptCardViewModelSlice,
            quickStartCardViewModelSlice,
            noCardsMessageViewModelSlice,
            quickLinksItemViewModelSlice,
            bloganuaryNudgeCardViewModelSlice,
            plansCardViewModelSlice,
            selectedSiteRepository
        )
    }

    @Test
    fun `when initialize is invoked, then should call initialize on required slices`() {
        val scope = testScope()

        dashboardCardsViewModelSlice.initialize(scope)

        verify(blazeCardViewModelSlice).initialize(scope)
        verify(bloggingPromptCardViewModelSlice).initialize(scope)
        verify(bloganuaryNudgeCardViewModelSlice).initialize(scope)
        verify(personalizeCardViewModelSlice).initialize(scope)
        verify(quickLinksItemViewModelSlice).initialization(scope)
        verify(cardViewModelSlice).initialize(scope)
        verify(quickStartCardViewModelSlice).initialize(scope)
    }

    @Test
    fun `given showDashboardCards is true, when buildCards, then should build cards`() = test {
        val mockSite = mock<SiteModel>()

        dashboardCardsViewModelSlice.initialize(testScope())
        dashboardCardsViewModelSlice.buildCards(mockSite)

        verify(jpMigrationSuccessCardViewModelSlice, atMost(1)).buildCard()
        verify(jetpackInstallFullPluginCardViewModelSlice, atMost(1)).buildCard(mockSite)
        verify(blazeCardViewModelSlice, atMost(1)).buildCard(mockSite)
        verify(bloggingPromptCardViewModelSlice, atMost(1)).fetchBloggingPrompt(mockSite)
        verify(bloganuaryNudgeCardViewModelSlice, atMost(1)).buildCard()
        verify(personalizeCardViewModelSlice, atMost(1)).buildCard()
        verify(quickLinksItemViewModelSlice, atMost(1)).buildCard(mockSite)
        verify(plansCardViewModelSlice, atMost(1)).buildCard(mockSite)
        verify(cardViewModelSlice, atMost(1)).buildCard(mockSite)
        verify(quickStartCardViewModelSlice, atMost(1)).build(mockSite)
    }

    @Test
    fun `when clear value called, then should clear value all slices`() {
        dashboardCardsViewModelSlice.initialize(testScope())
        dashboardCardsViewModelSlice.clearValue()

        verify(jpMigrationSuccessCardViewModelSlice).clearValue()
        verify(jetpackInstallFullPluginCardViewModelSlice).clearValue()
        verify(domainRegistrationCardViewModelSlice).clearValue()
        verify(blazeCardViewModelSlice).clearValue()
        verify(cardViewModelSlice).clearValue()
        verify(personalizeCardViewModelSlice).clearValue()
        verify(bloggingPromptCardViewModelSlice).clearValue()
        verify(quickStartCardViewModelSlice).clearValue()
        verify(quickLinksItemViewModelSlice).clearValue()
        verify(bloganuaryNudgeCardViewModelSlice).clearValue()
        verify(plansCardViewModelSlice).clearValue()
    }

    @Test
    fun `given initialized scope, when onCleared, then should cancel the coroutine scope`() {
        val scope = testScope()

        dashboardCardsViewModelSlice.initialize(scope)

        assertThat(scope.isActive).isTrue()

        dashboardCardsViewModelSlice.onCleared()

        verify(quickLinksItemViewModelSlice).onCleared()

        assertThat(scope.isActive).isFalse()
    }

    @Test
    fun `given selectedSite is not null, when refreshBloggingPrompt, then should build blogging prompt card`() {
        val mockSite = mock<SiteModel>()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSite)

        dashboardCardsViewModelSlice.refreshBloggingPrompt()

        verify(bloggingPromptCardViewModelSlice).fetchBloggingPrompt(mockSite)
    }

    @Test
    fun `given selectedSite is null, when refreshBloggingPrompt, then should not build blogging prompt card`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        dashboardCardsViewModelSlice.initialize(testScope())
        clearInvocations(bloggingPromptCardViewModelSlice)

        dashboardCardsViewModelSlice.refreshBloggingPrompt()

        verifyNoMoreInteractions(bloggingPromptCardViewModelSlice)
    }

    @Test
    fun `when resetShownTracker, then trackers are reset`() {
        dashboardCardsViewModelSlice.initialize(testScope())

        dashboardCardsViewModelSlice.resetShownTracker()

        verify(personalizeCardViewModelSlice).resetShown()
    }
}
