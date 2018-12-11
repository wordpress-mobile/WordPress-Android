package org.wordpress.android.ui.sitecreation.siteinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel.SiteInfoUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel.SiteInfoUiState.SkipNextButtonState

private const val SITE_TITLE = "Test Business Name"
private const val TAG_LINE = "Test Tag Line"

private val EMPTY_UI_STATE = SiteInfoUiState(siteTitle = "", tagLine = "")

class SiteInfoUiStateTest {
    @Test
    fun verifyStateIsSkipWhenEmpty() {
        assertThat(EMPTY_UI_STATE.skipButtonState).isEqualTo(SkipNextButtonState.SKIP)
    }

    @Test
    fun verifyStateIsNextWhenBusinessNameIsNotEmpty() {
        val uiStateWithBusinessName = EMPTY_UI_STATE.copy(siteTitle = SITE_TITLE)
        assertThat(uiStateWithBusinessName.skipButtonState).isEqualTo(SkipNextButtonState.NEXT)
    }

    @Test
    fun verifyStateIsNextWhenTagLineIsNotEmpty() {
        val uiStateWithTagLine = EMPTY_UI_STATE.copy(tagLine = TAG_LINE)
        assertThat(uiStateWithTagLine.skipButtonState).isEqualTo(SkipNextButtonState.NEXT)
    }
}
