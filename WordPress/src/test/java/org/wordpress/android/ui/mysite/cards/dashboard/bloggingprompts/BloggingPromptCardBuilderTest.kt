package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

private const val PROMPT_TITLE = "Test Prompt"
private const val NUMBER_OF_RESPONDENTS = 5

private val RESPONDENTS_IN_CARD = listOf(
    AvatarItem("http://avatar1.url"),
    AvatarItem("http://avatar2.url"),
    AvatarItem("http://avatar3.url"),
    TrailingLabelTextItem(
        UiString.UiStringPluralRes(
            0,
            R.string.my_site_blogging_prompt_card_number_of_answers_one,
            R.string.my_site_blogging_prompt_card_number_of_answers_other,
            NUMBER_OF_RESPONDENTS
        ),
        R.attr.colorOnSurface
    )
)

private val RESPONDENTS_IN_CARD_VIEW_ANSWERS = listOf(
    AvatarItem("http://avatar1.url"),
    AvatarItem("http://avatar2.url"),
    AvatarItem("http://avatar3.url"),
    TrailingLabelTextItem(
        UiStringRes(
            R.string.my_site_blogging_prompt_card_view_answers
        ),
        R.attr.colorOnSurface
    )
)

private val RESPONDENTS = listOf(
    "http://avatar1.url",
    "http://avatar2.url",
    "http://avatar3.url"
)

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BloggingPromptCardBuilderTest : BaseUnitTest() {
    private lateinit var builder: BloggingPromptCardBuilder

    @Suppress("MaxLineLength")
    private val bloggingPrompt = BloggingPromptModel(
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
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt)

        assertThat(statCard).isNotNull()
    }

    @Test
    fun `given blogging prompt, when card is built showing view more action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showViewMoreAction = true)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewMoreAction = true))
    }

    @Test
    fun `given blogging prompt, when card is built not showing view more action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showViewMoreAction = false)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewMoreAction = false))
    }

    @Test
    fun `given blogging prompt, when card is built showing view answers action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showViewAnswersAction = true)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewAnswersAction = true))
    }

    @Test
    fun `given blogging prompt, when card is built without showing view answers action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showViewAnswersAction = false)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showViewAnswersAction = false))
    }

    @Test
    fun `given blogging prompt, when card is built showing remove action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showRemoveAction = true)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showRemoveAction = true))
    }

    @Test
    fun `given blogging prompt, when card is built without remove answers action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt, showRemoveAction = false)

        assertThat(statCard).isEqualTo(bloggingPromptCard(showRemoveAction = false))
    }

    @Test
    fun `given no blogging prompt, when card is built, then return null`() {
        val statCard = buildBloggingPromptCardBuilderParams(null)

        assertThat(statCard).isNull()
    }

    private fun buildBloggingPromptCardBuilderParams(
        bloggingPrompt: BloggingPromptModel?,
        showViewMoreAction: Boolean = false,
        showViewAnswersAction: Boolean = false,
        showRemoveAction: Boolean = false,
    ) = builder.build(
        BloggingPromptCardBuilderParams(
            bloggingPrompt,
            showViewMoreAction,
            showViewAnswersAction,
            showRemoveAction,
            onShareClick,
            onAnswerClick,
            onSkipClick,
            onViewMoreClick,
            onViewAnswersClick,
            onRemoveClick,
        )
    )

    private val onShareClick: (message: String) -> Unit = { }
    private val onAnswerClick: (promptId: Int) -> Unit = { }
    private val onSkipClick: () -> Unit = { }
    private val onViewMoreClick: () -> Unit = { }
    private val onViewAnswersClick: (promptId: Int) -> Unit = { }
    private val onRemoveClick: () -> Unit = { }

    private fun bloggingPromptCard(
        showViewMoreAction: Boolean = false,
        showViewAnswersAction: Boolean = false,
        showRemoveAction: Boolean = false,
    ) = BloggingPromptCardWithData(
        prompt = UiStringText(PROMPT_TITLE),
        respondents = if (showViewAnswersAction) RESPONDENTS_IN_CARD_VIEW_ANSWERS else RESPONDENTS_IN_CARD,
        numberOfAnswers = NUMBER_OF_RESPONDENTS,
        false,
        promptId = 123,
        attribution = BloggingPromptAttribution.DAY_ONE,
        showViewMoreAction = showViewMoreAction,
        showRemoveAction = showRemoveAction,
        onShareClick = onShareClick,
        onAnswerClick = onAnswerClick,
        onSkipClick = onSkipClick,
        onViewMoreClick = onViewMoreClick,
        onViewAnswersClick = if (showViewAnswersAction) onViewAnswersClick else null,
        onRemoveClick = onRemoveClick,
    )
}
