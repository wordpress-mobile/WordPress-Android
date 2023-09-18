package org.wordpress.android.ui.mysite.cards.nocards

import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NoCardsMessageViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModelSlice: NoCardsMessageViewModelSlice

    @Before
    fun setUp() {
        viewModelSlice = NoCardsMessageViewModelSlice(
            analyticsTrackerWrapper
        )
    }

    @Test
    fun `given no cards, when build no cards message requested, then no cards message is built`() =
        test {
            val result = viewModelSlice.buildNoCardsMessage(emptyList())

            assertNotNull(result)
        }

    @Test
    fun `given dashboard card is empty, when build no cards message requested, then no cards message is built`() =
        test {
            val result = viewModelSlice.buildNoCardsMessage(
                listOf(
                    MySiteCardAndItem.Card.DashboardCards(
                        listOf()
                    )
                )
            )

            assertNotNull(result)
        }

    @Test
    fun `given cards, when build no cards message requested, then no cards message is not built`() =
        test {
            val result = viewModelSlice.buildNoCardsMessage(
                listOf(
                    mock<MySiteCardAndItem.Card.QuickStartCard>(),
                    mock<MySiteCardAndItem.Card.DomainRegistrationCard>(),
                    MySiteCardAndItem.Card.DashboardCards(
                        listOf(
                            mock(),
                        )
                    )
                )
            )
            assertNull(result)
        }

    @Test
    fun `given personalize card, when card shown track requested, then track card shown`() =
        test {
            viewModelSlice.trackShown(MySiteCardAndItem.Type.NO_CARDS_MESSAGE)

            verify(analyticsTrackerWrapper).track(
                AnalyticsTracker.Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                mapOf(
                    CardsTracker.TYPE to CardsTracker.Type.NO_CARDS.label,
                )
            )
        }
}
