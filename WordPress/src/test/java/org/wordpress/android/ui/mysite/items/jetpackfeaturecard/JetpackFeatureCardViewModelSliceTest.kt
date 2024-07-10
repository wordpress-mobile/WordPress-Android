package org.wordpress.android.ui.mysite.items.jetpackfeaturecard

import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardShownTracker
import kotlin.test.Test
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureCardViewModelSliceTest: BaseUnitTest() {
    @Mock
    lateinit var jetpackFeatureCardHelper: JetpackFeatureCardHelper

    @Mock
    lateinit var jetpackFeatureCardShownTracker: JetpackFeatureCardShownTracker

    private lateinit var viewModelSlice: JetpackFeatureCardViewModelSlice

    private lateinit var uiModels: MutableList<MySiteCardAndItem.Card.JetpackFeatureCard?>

    private lateinit var navigationEvents: MutableList<SiteNavigationAction>

    @Before
    fun setUp() {
        viewModelSlice = JetpackFeatureCardViewModelSlice(
            jetpackFeatureCardHelper,
            jetpackFeatureCardShownTracker
        )

        uiModels = mutableListOf()
        viewModelSlice.uiModel.observeForever {
            uiModels.add(it)
        }

        navigationEvents = mutableListOf()
        viewModelSlice.onNavigation.observeForever {
            it?.let { navigationEvents.add(it.peekContent()) }
        }
    }

    @Test
    fun `given jetpack feature card should not be shown, ui model is null`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(false)

        // when
        viewModelSlice.buildJetpackFeatureCard()

        // then
        assertNull(uiModels.last())
    }

    @Test
    fun `given jetpack feature card should be shown, ui model is not null`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()

        // then
        assertNotNull(uiModels.last())
    }

    @Test
    fun `given jetpack feature card should be shown, onJetpackFeatureCardClick is called`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()
        uiModels.last()?.onClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_TAPPED)
        assertThat(navigationEvents.last()).isInstanceOf(SiteNavigationAction.OpenJetpackFeatureOverlay::class.java)
    }

    @Test
    fun `given jetpack feature card should be shown, onJetpackFeatureCardHideMenuItemClick is called`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()
        uiModels.last()?.onHideMenuItemClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).hideJetpackFeatureCard()
        assertNull(uiModels.last())
    }

    @Test
    fun `given jetpack feature card should be shown, onJetpackFeatureCardLearnMoreClick is called`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()
        uiModels.last()?.onLearnMoreClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_LINK_TAPPED)
        assertThat(navigationEvents.last()).isInstanceOf(SiteNavigationAction.OpenJetpackFeatureOverlay::class.java)
    }

    @Test
    fun `given jetpack feature card should be shown, onJetpackFeatureCardRemindMeLaterClick is called`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()
        uiModels.last()?.onRemindMeLaterItemClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).setJetpackFeatureCardLastShownTimeStamp(any())
        assertNull(uiModels.last())
    }

    @Test
    fun `given jetpack feature card should be shown, onJetpackFeatureCardMoreMenuClick is called`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackFeatureCard()
        uiModels.last()?.onMoreMenuClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }

    @Test
    fun `when trackshown is called, then jetpackFeatureCardShownTracker is called`() = test {
        // when
        viewModelSlice.trackShown(mock())

        // then
        verify(jetpackFeatureCardShownTracker).trackShown(any())
    }

    @Test
    fun `when clearValue is called, then ui model is null`() = test {
        // when
        viewModelSlice.clearValue()

        // then
        assertNull(uiModels.last())
    }
}
