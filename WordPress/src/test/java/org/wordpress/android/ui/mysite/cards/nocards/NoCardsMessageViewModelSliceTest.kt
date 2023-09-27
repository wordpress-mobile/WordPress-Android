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
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.DashboardPersonalizationFeatureConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NoCardsMessageViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var dashboardPersonalizationFeatureConfig: DashboardPersonalizationFeatureConfig

    private lateinit var viewModelSlice: NoCardsMessageViewModelSlice

    @Before
    fun setUp() {
        viewModelSlice = NoCardsMessageViewModelSlice(
            analyticsTrackerWrapper,
            dashboardPersonalizationFeatureConfig
        )
    }

    @Test
    fun `given no cards + FF off, when build no cards message requested, then no cards message is built`() =
        test {
            whenever(dashboardPersonalizationFeatureConfig.isEnabled()).thenReturn(false)

            val result = viewModelSlice.buildNoCardsMessage(emptyList())

            assertNull(result)
        }

    @Test
    fun `given no cards + FF on, when build no cards message requested, then no cards message is built`() =
        test {
            whenever(dashboardPersonalizationFeatureConfig.isEnabled()).thenReturn(true)

            val result = viewModelSlice.buildNoCardsMessage(emptyList())

            assertNotNull(result)
        }

    @Test
    fun `given dashboard card is empty, when build no cards message requested, then no cards message is built`() =
        test {
            whenever(dashboardPersonalizationFeatureConfig.isEnabled()).thenReturn(true)

            val result = viewModelSlice.buildNoCardsMessage(
                listOf(
                    mock<MySiteCardAndItem.Card.ActivityCard.ActivityCardWithItems>()
                )
            )

            assertNotNull(result)
        }

    @Test
    fun `given cards, when build no cards message requested, then no cards message is not built`() =
        test {
            whenever(dashboardPersonalizationFeatureConfig.isEnabled()).thenReturn(true)

            val result = viewModelSlice.buildNoCardsMessage(
                listOf(
                    mock<MySiteCardAndItem.Card.QuickStartCard>().apply {
                        whenever(type).thenReturn(MySiteCardAndItem.Type.QUICK_START_CARD) },
                    mock<MySiteCardAndItem.Card.DomainRegistrationCard>().apply {
                        whenever(type).thenReturn(MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD) },
                    mock<MySiteCardAndItem.Card.ActivityCard.ActivityCardWithItems>().apply {
                        whenever(type).thenReturn(MySiteCardAndItem.Type.ACTIVITY_CARD) }
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
