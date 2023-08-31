package org.wordpress.android.ui.mysite.cards.dashboard.activity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams.ActivityCardItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ActivityLogCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var activityLogCardViewModelSlice: ActivityLogCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var refreshEvents: MutableList<Boolean>

    private val site = mock<SiteModel>()

    private val activityId = "activityId"
    private val isRewindable = false

    @Before
    fun setUp() {
        activityLogCardViewModelSlice = ActivityLogCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            appPrefsWrapper
        )
        navigationActions = mutableListOf()
        activityLogCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        refreshEvents = mutableListOf()
        activityLogCardViewModelSlice.refresh.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                refreshEvents.add(it)
            }
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `given activity log card, when card item is clicked, then stats page is opened`() =
        test {
            val params = activityLogCardViewModelSlice.getActivityLogCardBuilderParams(mock())
            val cardItemClickParams = ActivityCardItemClickParams(activityId, isRewindable)

            params.onActivityItemClick(cardItemClickParams)

            Assertions.assertThat(navigationActions).containsOnly(
                SiteNavigationAction.OpenActivityLogDetail(
                    site,
                    activityId,
                    isRewindable
                )
            )
        }
}
