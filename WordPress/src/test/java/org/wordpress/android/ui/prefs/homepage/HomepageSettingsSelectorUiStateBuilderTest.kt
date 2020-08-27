package org.wordpress.android.ui.prefs.homepage

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsSelectorUiState.PageUiModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class HomepageSettingsSelectorUiStateBuilderTest {
    private val site = SiteModel()
    private val pageA = buildPage(1, 1L, "title A")
    private val pageB = buildPage(2, 2L, "title B")

    @Test
    fun `builds a selector UI state without a selected page`() {
        val uiState = HomepageSettingsSelectorUiState.Builder.build(listOf(pageA, pageB), null)

        assertThat(uiState.data).containsOnly(
                PageUiModel(pageA.pageId, pageA.remoteId, pageA.title),
                PageUiModel(pageB.pageId, pageB.remoteId, pageB.title)
        )
        assertThat(uiState.isExpanded).isFalse()
        assertThat(uiState.isHighlighted).isFalse()
        assertThat(uiState.selectedItem).isEqualTo(UiStringRes(R.string.site_settings_select_page))
        assertThat(uiState.selectedItemId).isEqualTo(0)
    }

    @Test
    fun `builds a selector UI state with a selected page`() {
        val uiState = HomepageSettingsSelectorUiState.Builder.build(listOf(pageA, pageB), pageA.remoteId)

        assertThat(uiState.data).containsOnly(
                PageUiModel(pageA.pageId, pageA.remoteId, pageA.title),
                PageUiModel(pageB.pageId, pageB.remoteId, pageB.title)
        )
        assertThat(uiState.isExpanded).isFalse()
        assertThat(uiState.isHighlighted).isTrue()
        assertThat(uiState.selectedItem).isEqualTo(UiStringText(pageA.title))
        assertThat(uiState.selectedItemId).isEqualTo(pageA.remoteId)
    }

    private fun buildPage(localId: Int, remoteId: Long, title: String) =
            PageModel(
                    mock(),
                    site,
                    localId,
                    title,
                    PUBLISHED,
                    Date(),
                    false,
                    remoteId,
                    null,
                    -1
            )
}
