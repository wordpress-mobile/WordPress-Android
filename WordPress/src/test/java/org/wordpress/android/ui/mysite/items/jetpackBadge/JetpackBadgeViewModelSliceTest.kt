package org.wordpress.android.ui.mysite.items.jetpackBadge

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.JetpackBrandingUtils

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackBadgeViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    private lateinit var viewModelSlice: JetpackBadgeViewModelSlice

    private lateinit var uiModels: MutableList<MySiteCardAndItem.JetpackBadge?>

    private lateinit var navigationEvents: MutableList<SiteNavigationAction>

    @Before
    fun setUp() {
        viewModelSlice = JetpackBadgeViewModelSlice(jetpackBrandingUtils)

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
    fun `given jetpack branding should not be shown, ui model is null`() = test {
        // given
        whenever(jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard()).thenReturn(false)

        // when
        viewModelSlice.buildJetpackBadge()
        advanceUntilIdle()

        // then
        assertNull(uiModels[0])
    }

    @Test
    fun `given jetpack branding should be shown, ui model is not null`() = test {
        // given
        val brandingText = UiString.UiStringText("Jetpack")
        whenever(jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard()).thenReturn(true)
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(true)
        whenever(jetpackBrandingUtils.getBrandingTextForScreen(JetpackPoweredScreen.WithStaticText.HOME))
            .thenReturn(brandingText)

        // when
        viewModelSlice.buildJetpackBadge()

        // then
        assertEquals(1, uiModels.size)
        assertEquals(brandingText, uiModels[0]?.text)
    }

    @Test
    fun `given jetpack branding should be shown, when badge is clicked, then navigation event is emitted`() = test {
        // given
        val brandingText = UiString.UiStringText("Jetpack")
        whenever(jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard()).thenReturn(true)
        whenever(jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()).thenReturn(true)
        whenever(jetpackBrandingUtils.getBrandingTextForScreen(JetpackPoweredScreen.WithStaticText.HOME))
            .thenReturn(brandingText)

        // when
        viewModelSlice.buildJetpackBadge()
        uiModels[0]?.onClick?.click()

        // then
        assertEquals(1, navigationEvents.size)
        assertEquals(SiteNavigationAction.OpenJetpackPoweredBottomSheet, navigationEvents[0])
    }
}

