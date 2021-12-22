package org.wordpress.android.ui.mysite.cards

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SITE_INFO_CARD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class CardsShownTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var cardsShownTracker: CardsShownTracker

    @Before
    fun setUp() {
        cardsShownTracker = CardsShownTracker(analyticsTracker)
    }

    @Test
    fun `when domain card is shown, then domain registration is tracked`() {
        cardsShownTracker.trackCardShown(itemType = DOMAIN_REGISTRATION_CARD)

        verify(analyticsTracker).track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN)
    }

    @Test
    fun `when domain card is not shown, then domain registration is not tracked`() {
        cardsShownTracker.trackCardShown(itemType = SITE_INFO_CARD)

        verify(analyticsTracker, never()).track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN)
    }
}
