package org.wordpress.android.ui.mysite.cards.personalize

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PersonalizeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var personalizeCardShownTracker: PersonalizeCardShownTracker

    @Mock
    lateinit var personalizeCardBuilder: PersonalizeCardBuilder

    private lateinit var viewModelSlice: PersonalizeCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    @Before
    fun setUp() {
        viewModelSlice = PersonalizeCardViewModelSlice(
            cardsTracker,
            personalizeCardShownTracker,
            personalizeCardBuilder
        )
        navigationActions = mutableListOf()
        viewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
    }

    @Test
    fun `given personalize card, when card item is clicked, then event is tracked`() =
        test {
            val params = viewModelSlice.getBuilderParams()

            params.onClick()

            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.PERSONALIZE_CARD.label, CardsTracker.Type.PERSONALIZE_CARD.label)
        }

    @Test
    fun `given personalize card, when card item is clicked, then open personalize event is raised`() =
        test {
            val params = viewModelSlice.getBuilderParams()

            params.onClick()

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenDashboardPersonalization)
        }

    @Test
    fun `given personalize card, when card shown track requested, then track card shown`() =
        test {
            viewModelSlice.trackShown()

            verify(personalizeCardShownTracker).trackShown(MySiteCardAndItem.Type.PERSONALIZE_CARD)
        }

    @Test
    fun `given personalize card, when reset shown tracker requested, then track card shown is reset`() =
        test {
            viewModelSlice.resetShown()

            verify(personalizeCardShownTracker).resetShown()
        }
}
