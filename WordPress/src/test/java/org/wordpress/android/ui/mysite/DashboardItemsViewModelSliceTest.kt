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
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.atMost
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.items.DashboardItemsViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackBadge.JetpackBadgeViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackSwitchmenu.JetpackSwitchMenuViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackfeaturecard.JetpackFeatureCardViewModelSlice
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.BuildConfigWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DashboardItemsViewModelSliceTest: BaseUnitTest() {
    @Mock
    lateinit var jetpackFeatureCardViewModelSlice: JetpackFeatureCardViewModelSlice

    @Mock
    lateinit var jetpackSwitchMenuViewModelSlice: JetpackSwitchMenuViewModelSlice

    @Mock
    lateinit var jetpackBadgeViewModelSlice: JetpackBadgeViewModelSlice

    @Mock
    lateinit var siteItemsViewModelSlice: SiteItemsViewModelSlice

    @Mock
    lateinit var sotw2023NudgeCardViewModelSlice: WpSotw2023NudgeCardViewModelSlice

    @Mock
    lateinit var jetpackFeatureCardHelper: JetpackFeatureCardHelper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    private lateinit var dashboardItemsViewModelSlice: DashboardItemsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var snackBarMessages: MutableList<SnackbarMessageHolder>
    private lateinit var uiModels: MutableList<List<MySiteCardAndItem?>>

    @Before
    fun setup() {
        whenever(jetpackFeatureCardViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(jetpackSwitchMenuViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(jetpackBadgeViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(siteItemsViewModelSlice.uiModel).thenReturn(MutableLiveData())
        whenever(sotw2023NudgeCardViewModelSlice.uiModel).thenReturn(MutableLiveData())

        dashboardItemsViewModelSlice = DashboardItemsViewModelSlice(
            testDispatcher(),
            jetpackFeatureCardViewModelSlice,
            jetpackSwitchMenuViewModelSlice,
            jetpackBadgeViewModelSlice,
            siteItemsViewModelSlice,
            sotw2023NudgeCardViewModelSlice,
            jetpackFeatureCardHelper
        )

        navigationActions = mutableListOf()
        snackBarMessages = mutableListOf()
        uiModels = mutableListOf()

        dashboardItemsViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let { navigationActions.add(it) }
        }

        dashboardItemsViewModelSlice.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let { snackBarMessages.add(it) }
        }

        dashboardItemsViewModelSlice.uiModel.observeForever { uiModel ->
            uiModels.add(uiModel)
        }
    }


    @Test
    fun `when initialize is invoked then should call initialize on sotw2023NudgeCardViewModelSlice`() {
        val scope = testScope()
        dashboardItemsViewModelSlice.initialize(scope)
        verify(sotw2023NudgeCardViewModelSlice).initialize(scope)
    }

    @Test
    fun `when build invoked, then should build cards`() = test {
        val mockSite = mock<SiteModel>()

        dashboardItemsViewModelSlice.initialize(testScope())
        dashboardItemsViewModelSlice.buildItems(mockSite)

        verify(siteItemsViewModelSlice, atLeastOnce()).buildSiteItems(any())
        verify(jetpackFeatureCardViewModelSlice, atMost(1)).buildJetpackFeatureCard()
        verify(jetpackSwitchMenuViewModelSlice, atMost(1)).buildJetpackSwitchMenu()
        verify(jetpackBadgeViewModelSlice, atMost(1)).buildJetpackBadge()
        verify(siteItemsViewModelSlice, atMost(1)).buildSiteItems(mockSite)
        verify(sotw2023NudgeCardViewModelSlice, atMost(1)).buildCard()
    }

    @Test
    fun `when clear value invoked, then should clear vm slices value`() = test {
        dashboardItemsViewModelSlice.initialize(testScope())
        dashboardItemsViewModelSlice.clearValue()

        verify(siteItemsViewModelSlice).clearValue()
        verify(jetpackFeatureCardViewModelSlice).clearValue()
        verify(jetpackSwitchMenuViewModelSlice).clearValue()
        verify(jetpackBadgeViewModelSlice).clearValue()
        verify(sotw2023NudgeCardViewModelSlice).clearValue()
    }

    @Test
    fun `given initialized scope, when onCleared, then should cancel the coroutine scope`() {
        val scope = testScope()
        dashboardItemsViewModelSlice.initialize(scope)

        // Verify that scope is not canceled before calling onCleared
        assertThat(scope.isActive).isTrue()

        dashboardItemsViewModelSlice.onCleared()

        // Verify that the scope is canceled after calling onCleared
        assertThat(scope.isActive).isFalse()
    }
}
