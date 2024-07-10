package org.wordpress.android.ui.mysite.items.jetpackSwitchmenu

import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import kotlin.test.Test
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackSwitchMenuViewModelSliceTest: BaseUnitTest(){
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var jetpackFeatureCardHelper: JetpackFeatureCardHelper

    private lateinit var viewModelSlice: JetpackSwitchMenuViewModelSlice

    private lateinit var uiModels: MutableList<MySiteCardAndItem.Card.JetpackSwitchMenu?>

    private lateinit var navigationEvents: MutableList<SiteNavigationAction>

    @Before
    fun setUp() {
        viewModelSlice = JetpackSwitchMenuViewModelSlice(
            jetpackFeatureCardHelper,
            appPrefsWrapper
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
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(false)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()

        // then
        assertNull(uiModels[0])
    }

    @Test
    fun `given jetpack feature card should be shown, ui model is not null`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()

        // then
        assertNotNull(uiModels[0])
    }

    @Test
    fun `given card shown, when clicked, then navigation event is emitted`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()
        uiModels[0]?.onClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_TAPPED)
        assertThat(navigationEvents[0]).isInstanceOf(SiteNavigationAction.OpenJetpackFeatureOverlay::class.java)
    }

    @Test
    fun `given card shown, when remind me later clicked, then ui model is null`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()
        uiModels[0]?.onRemindMeLaterItemClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_REMIND_LATER_TAPPED)
        verify(appPrefsWrapper).setSwitchToJetpackMenuCardLastShownTimestamp(any())
        assertNull(uiModels[1])
    }

    @Test
    fun `given card shown, when hide menu item clicked, then ui model is null`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()
        uiModels[0]?.onHideMenuItemClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).hideSwitchToJetpackMenuCard()
        assertNull(uiModels[1])
    }

    @Test
    fun `given card shown, when more menu clicked, then analytics is tracked`() = test {
        // given
        whenever(jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()).thenReturn(true)

        // when
        viewModelSlice.buildJetpackSwitchMenu()
        advanceUntilIdle()
        uiModels[0]?.onMoreMenuClick?.click()
        advanceUntilIdle()

        // then
        verify(jetpackFeatureCardHelper).track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }
}
