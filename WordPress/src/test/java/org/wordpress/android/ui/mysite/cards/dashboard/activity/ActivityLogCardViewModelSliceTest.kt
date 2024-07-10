package org.wordpress.android.ui.mysite.cards.dashboard.activity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
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

    @Mock
    lateinit var activityCardBuilder: ActivityCardBuilder

    private lateinit var activityLogCardViewModelSlice: ActivityLogCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var uiModels : MutableList<MySiteCardAndItem.Card.ActivityCard?>

    private val site = mock<SiteModel>()

    private val activityId = "activityId"
    private val isRewindable = false

    @Before
    fun setUp() {
        activityLogCardViewModelSlice = ActivityLogCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            appPrefsWrapper,
            activityCardBuilder
        )
        navigationActions = mutableListOf()
        activityLogCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        uiModels = mutableListOf()
        activityLogCardViewModelSlice.uiModel.observeForever { uiModel ->
            uiModels.add(uiModel)
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

    @Test
    fun `given activity log card, when card more menu is clicked, then event is tracked`() =
        test {
            val params = activityLogCardViewModelSlice.getActivityLogCardBuilderParams(mock())

            params.onMoreMenuClick()

            verify(cardsTracker).trackCardMoreMenuClicked(CardsTracker.Type.ACTIVITY.label)
        }

    @Test
    fun `given activity log card, when all activity menu item is clicked, all activity page is opened`() =
        test {
            val params = activityLogCardViewModelSlice.getActivityLogCardBuilderParams(mock())

            params.onAllActivityMenuItemClick()

            Assertions.assertThat(navigationActions).containsOnly(
                SiteNavigationAction.OpenActivityLog(site)
            )
            verify(cardsTracker).trackCardMoreMenuItemClicked(
                CardsTracker.Type.ACTIVITY.label,
                ActivityLogCardViewModelSlice.MenuItemType.ALL_ACTIVITY.label
            )
        }

    @Test
    fun `given activity log card, when hide menu item is clicked, all activity page is opened`() =
        test {
            val params = activityLogCardViewModelSlice.getActivityLogCardBuilderParams(mock())

            params.onHideMenuItemClick()

            verify(cardsTracker).trackCardMoreMenuItemClicked(
                CardsTracker.Type.ACTIVITY.label,
                ActivityLogCardViewModelSlice.MenuItemType.HIDE_THIS.label
            )
            verify(appPrefsWrapper).setShouldHideActivityDashboardCard(any(), any())
            Assertions.assertThat(uiModels.last()).isNull()
        }
}
