package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

private const val PROMPT_TITLE = "Test Prompt"
private const val NUMBER_OF_RESPONDENTS = 5

private val RESPONDENTS_IN_CARD = listOf(
        AvatarItem("http://avatar1.url"),
        AvatarItem("http://avatar2.url"),
        AvatarItem("http://avatar3.url"),
        TrailingLabelTextItem(
                UiStringPluralRes(
                        0,
                        R.string.my_site_blogging_prompt_card_number_of_answers_one,
                        R.string.my_site_blogging_prompt_card_number_of_answers_other,
                        NUMBER_OF_RESPONDENTS
                ), attr.colorOnSurface
        )
)

private val RESPONDENTS = listOf(
        "http://avatar1.url",
        "http://avatar2.url",
        "http://avatar3.url"
)

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptCardBuilderTest : BaseUnitTest() {
    private lateinit var builder: BloggingPromptCardBuilder
    @Suppress("MaxLineLength") private val bloggingPrompt = BloggingPromptModel(
            id = 123,
            text = PROMPT_TITLE,
            title = "",
            content = "<!-- wp:pullquote -->\n" +
                    "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the whole world live (on television or radio — choose your format). What would you say?</p><cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
                    "<!-- /wp:pullquote -->",
            date = Date(),
            isAnswered = false,
            attribution = "dayone",
            respondentsCount = 5,
            respondentsAvatarUrls = RESPONDENTS
    )

    @Before
    fun setUp() {
        builder = BloggingPromptCardBuilder()
    }

    @Test
    fun `given blogging prompt, when card is built, then return card`() {
        val statCard = buildBloggingPromptCard(bloggingPrompt)

        assertThat(statCard).isNotNull()
    }

    @Test
    fun `given blogging prompt, when card is built showing view more action, then return matching card`() {
        val statCard = buildBloggingPromptCard(bloggingPrompt, showViewMoreAction = true)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewMoreAction = true))
    }

    @Test
    fun `given blogging prompt, when card is built not showing view more action, then return matching card`() {
        val statCard = buildBloggingPromptCard(bloggingPrompt, showViewMoreAction = false)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewMoreAction = false))
    }

    @Test
    fun `given no blogging prompt, when card is built, then return null`() {
        val statCard = buildBloggingPromptCard(null)

        assertThat(statCard).isNull()
    }

    private fun buildBloggingPromptCard(
        bloggingPrompt: BloggingPromptModel?,
        showViewMoreAction: Boolean = false
    ) = builder.build(
            BloggingPromptCardBuilderParams(
                    bloggingPrompt,
                    showViewMoreAction,
                    onShareClick,
                    onAnswerClick,
                    onSkipClick
            )
    )

    private val onShareClick: (message: String) -> Unit = { }
    private val onAnswerClick: (promptId: Int) -> Unit = { }
    private val onSkipClick: () -> Unit = { }

    private fun bloggingPromptCard(showViewMoreAction: Boolean = false) = BloggingPromptCardWithData(
            prompt = UiStringText(PROMPT_TITLE),
            respondents = RESPONDENTS_IN_CARD,
            numberOfAnswers = NUMBER_OF_RESPONDENTS,
            false,
            promptId = 123,
            attribution = BloggingPromptAttribution.DAY_ONE,
            showViewMoreAction = showViewMoreAction,
            onShareClick = onShareClick,
            onAnswerClick = onAnswerClick,
            onSkipClick = onSkipClick,
    )
}
