package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.utils.UiString

class BloganuaryNudgeCardBuilderTest {
    @Test
    fun `GIVEN not eligible, WHEN build is called, THEN return null`() {
        val params = BloganuaryNudgeCardBuilderParams(
            title = UiString.UiStringText("title"),
            text = UiString.UiStringText("text"),
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
            title = UiString.UiStringText("title"),
            text = UiString.UiStringText("text"),
            isEligible = true,
            onLearnMoreClick = { currentAction = "onLearnMoreClick" },
            onMoreMenuClick = { currentAction = "onMoreMenuClick" },
            onHideMenuItemClick = { currentAction = "onHideMenuItemClick" },
        )
        val cardModel = BloganuaryNudgeCardBuilder().build(params)

        assertThat(cardModel!!).isNotNull
        assertThat(cardModel.title).isEqualTo(UiString.UiStringText("title"))
        assertThat(cardModel.text).isEqualTo(UiString.UiStringText("text"))

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
