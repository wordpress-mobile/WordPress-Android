package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.R

class BloganuaryNudgeCardBuilderTest {
    @Test
    fun `GIVEN not eligible, WHEN build is called, THEN return null`() {
        val params = BloganuaryNudgeCardBuilderParams(
            isEligible = false,
            onLearnMoreClick = {},
            onMoreMenuClick = {},
            onHideMenuItemClick = {},
        )
        val cardModel = BloganuaryNudgeCardBuilder().build(params)
        assertThat(cardModel).isNull()
    }

    @Test
    fun `GIVEN eligible, WHEN build is called, THEN return correct model`() {
        var currentAction = ""

        val params = BloganuaryNudgeCardBuilderParams(
            isEligible = true,
            onLearnMoreClick = { currentAction = "onLearnMoreClick" },
            onMoreMenuClick = { currentAction = "onMoreMenuClick" },
            onHideMenuItemClick = { currentAction = "onHideMenuItemClick" },
        )
        val cardModel = BloganuaryNudgeCardBuilder().build(params)

        assertThat(cardModel!!).isNotNull
        assertThat(cardModel.title).isEqualTo(UiString.UiStringRes(R.string.bloganuary_dashboard_nudge_title))
        assertThat(cardModel.text).isEqualTo(UiString.UiStringRes(R.string.bloganuary_dashboard_nudge_text))

        // check if the callbacks are hooked correctly
        assertThat(currentAction).isEmpty()
        cardModel.onLearnMoreClick.click()
        assertThat(currentAction).isEqualTo("onLearnMoreClick")
        cardModel.onMoreMenuClick.click()
        assertThat(currentAction).isEqualTo("onMoreMenuClick")
        cardModel.onHideMenuItemClick.click()
        assertThat(currentAction).isEqualTo("onHideMenuItemClick")
    }
}
