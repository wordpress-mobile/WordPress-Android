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
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

private const val PROMPT_TITLE = "Test Prompt"
private const val NUMBER_OF_RESPONDENTS = 5

private val RESPONDENTS_IN_CARD_VIEW_ANSWERS = listOf(
    AvatarItem("http://avatar1.url"),
    AvatarItem("http://avatar2.url"),
    AvatarItem("http://avatar3.url"),
    TrailingLabelTextItem(
        UiStringRes(
            R.string.my_site_blogging_prompt_card_view_answers
        ),
        R.color.primary_emphasis_medium_selector
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
        date = Date(),
        isAnswered = false,
        attribution = "dayone",
        respondentsCount = 5,
        respondentsAvatarUrls = RESPONDENTS,
        answeredLink = "https://wordpress.com/tag/dailyprompt-123"
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
    fun `given blogging prompt, when card is built without showing view answers action, then return matching card`() {
        val statCard = buildBloggingPromptCardBuilderParams(bloggingPrompt)

        assertThat(statCard).isEqualTo(bloggingPromptCard())
    }

    @Test
    fun `given no blogging prompt, when card is built, then return null`() {
        val statCard = buildBloggingPromptCardBuilderParams(null)

        assertThat(statCard).isNull()
    }

    private fun buildBloggingPromptCardBuilderParams(
        bloggingPrompt: BloggingPromptModel?
    ) = builder.build(
        BloggingPromptCardBuilderParams(
            bloggingPrompt,
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
    private val onViewAnswersClick: (tagUrl: String) -> Unit = { }
    private val onRemoveClick: () -> Unit = { }

    private fun bloggingPromptCard(
    ) = BloggingPromptCardWithData(
        prompt = UiStringText(PROMPT_TITLE),
        respondents = RESPONDENTS_IN_CARD_VIEW_ANSWERS,
        numberOfAnswers = NUMBER_OF_RESPONDENTS,
        false,
        promptId = 123,
        tagUrl = "https://wordpress.com/tag/dailyprompt-123",
        attribution = BloggingPromptAttribution.DAY_ONE,
        onShareClick = onShareClick,
        onAnswerClick = onAnswerClick,
        onSkipClick = onSkipClick,
        onViewMoreClick = onViewMoreClick,
        onViewAnswersClick = onViewAnswersClick,
        onRemoveClick = onRemoveClick,
    )
}
