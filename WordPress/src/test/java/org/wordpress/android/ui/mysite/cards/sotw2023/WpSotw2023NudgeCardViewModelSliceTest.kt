package org.wordpress.android.ui.mysite.cards.sotw2023

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore

import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.util.config.WpSotw2023NudgeFeatureConfig

@OptIn(ExperimentalCoroutinesApi::class)
class WpSotw2023NudgeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var featureConfig: WpSotw2023NudgeFeatureConfig

    private lateinit var viewModelSlice: WpSotw2023NudgeCardViewModelSlice

    @Before
    fun setUp() {
        viewModelSlice = WpSotw2023NudgeCardViewModelSlice(featureConfig)
        viewModelSlice.initialize(testScope())
    }

    @Test
    fun `WHEN feature is disabled THEN buildCard returns null `() {
        whenever(featureConfig.isEnabled()).thenReturn(false)

        val card = viewModelSlice.buildCard()

        assertThat(card).isNull()
    }

    @Test
    fun `WHEN feature is enabled THEN buildCard returns card `() {
        whenever(featureConfig.isEnabled()).thenReturn(true)

        val card = viewModelSlice.buildCard()

        assertThat(card!!).isNotNull
    }

    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN hide card in app prefs and refresh`() {
        // TODO thomashortadev implement when done
    }

    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onCtaClick is clicked THEN navigate to URL`() {
        // TODO thomashortadev implement when done
    }

    // region Analytics
    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onHideMenuItemClick is clicked THEN analytics is tracked`() {
        // TODO thomashortadev implement when done
    }

    @Ignore("TODO thomashortadev")
    @Test
    fun `WHEN card onCtaClick is clicked THEN analytics is tracked`() {
        // TODO thomashortadev implement when done
    }
    // endregion Analytics
}
