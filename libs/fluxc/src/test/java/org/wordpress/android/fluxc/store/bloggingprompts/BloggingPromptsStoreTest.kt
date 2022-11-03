package org.wordpress.android.fluxc.store.bloggingprompts

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsRespondentAvatar
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

const val SITE_LOCAL_ID = 1

private val PROMPTS_RESPONSE = BloggingPromptsListResponse(
    prompts = listOf(
        BloggingPromptResponse(
            id = 1,
            text = "Cast the movie of your life.",
            title = "Prompt Title",
            content = "content of the prompt",
            date = "2015-01-12",
            attribution = "",
            isAnswered = false,
            respondentsCount = 0,
            respondentsAvatars = emptyList()
        ),

        BloggingPromptResponse(
            id = 2,
            text = "Cast the movie of your life 2.",
            title = "Prompt Title 2",
            content = "content of the prompt 2",
            date = "2015-01-13",
            attribution = "dayone",
            isAnswered = true,
            respondentsCount = 1,
            respondentsAvatars = listOf(BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"))
        ),
        BloggingPromptResponse(
            id = 3,
            text = "Cast the movie of your life 3.",
            title = "Prompt Title 3",
            content = "content of the prompt 3",
            date = "2015-01-14",
            attribution = "",
            isAnswered = false,
            respondentsCount = 3,
            respondentsAvatars = listOf(
                BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"),
                BloggingPromptsRespondentAvatar("http://site/avatar2.jpg"),
                BloggingPromptsRespondentAvatar("http://site/avatar3.jpg")
            )
        )
    )
)

/* MODEL */

private val FIRST_PROMPT_MODEL = BloggingPromptModel(
    id = 1,
    text = "Cast the movie of your life.",
    title = "Prompt Title",
    content = "content of the prompt",
    date = BloggingPromptsUtils.stringToDate("2015-01-12"),
    attribution = "",
    isAnswered = false,
    respondentsCount = 0,
    respondentsAvatarUrls = emptyList()
)

private val SECOND_PROMPT_MODEL = BloggingPromptModel(
    id = 2,
    text = "Cast the movie of your life 2.",
    title = "Prompt Title 2",
    content = "content of the prompt 2",
    date = BloggingPromptsUtils.stringToDate("2015-01-13"),
    attribution = "dayone",
    isAnswered = true,
    respondentsCount = 1,
    respondentsAvatarUrls = listOf("http://site/avatar1.jpg")
)

private val THIRD_PROMPT_MODEL = BloggingPromptModel(
    id = 3,
    text = "Cast the movie of your life 3.",
    title = "Prompt Title 3",
    content = "content of the prompt 3",
    date = BloggingPromptsUtils.stringToDate("2015-01-14"),
    attribution = "",
    isAnswered = false,
    respondentsCount = 3,
    respondentsAvatarUrls = listOf(
        "http://site/avatar1.jpg",
        "http://site/avatar2.jpg",
        "http://site/avatar3.jpg"
    )
)

private val PROMPT_MODELS = listOf(FIRST_PROMPT_MODEL, SECOND_PROMPT_MODEL, THIRD_PROMPT_MODEL)

/* ENTITY */

private val FIRST_PROMPT_ENTITY = BloggingPromptEntity(
    id = 1,
    siteLocalId = SITE_LOCAL_ID,
    text = "Cast the movie of your life.",
    title = "Prompt Title",
    content = "content of the prompt",
    date = BloggingPromptsUtils.stringToDate("2015-01-12"),
    attribution = "",
    isAnswered = false,
    respondentsCount = 0,
    respondentsAvatars = emptyList()
)

private val SECOND_PROMPT_ENTITY = BloggingPromptEntity(
    id = 2,
    siteLocalId = SITE_LOCAL_ID,
    text = "Cast the movie of your life 2.",
    title = "Prompt Title 2",
    content = "content of the prompt 2",
    date = BloggingPromptsUtils.stringToDate("2015-01-13"),
    attribution = "dayone",
    isAnswered = true,
    respondentsCount = 1,
    respondentsAvatars = listOf("http://site/avatar1.jpg")
)

private val THIRD_PROMPT_ENTITY = BloggingPromptEntity(
    id = 3,
    siteLocalId = SITE_LOCAL_ID,
    text = "Cast the movie of your life 3.",
    title = "Prompt Title 3",
    content = "content of the prompt 3",
    date = BloggingPromptsUtils.stringToDate("2015-01-14"),
    attribution = "",
    isAnswered = false,
    respondentsCount = 3,
    respondentsAvatars = listOf(
        "http://site/avatar1.jpg",
        "http://site/avatar2.jpg",
        "http://site/avatar3.jpg"
    )
)

private val PROMPT_ENTITIES = listOf(FIRST_PROMPT_ENTITY, SECOND_PROMPT_ENTITY, THIRD_PROMPT_ENTITY)

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptsStoreTest {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: BloggingPromptsRestClient
    @Mock private lateinit var dao: BloggingPromptsDao

    private lateinit var promptsStore: BloggingPromptsStore

    private val numberOfPromptsToFetch = 40
    private val requestedPromptDate = Date()

    @Before
    fun setUp() {
        promptsStore = BloggingPromptsStore(
            restClient,
            dao,
            initCoroutineEngine()
        )
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
    }

    @Test
    fun `when fetch prompts triggered, then all prompt model are inserted into db`() = test {
        val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
        whenever(
            restClient.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate
            )
        ).thenReturn(payload)

        promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)

        verify(dao).insertForSite(siteModel.id, PROMPT_MODELS)
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then empty cards model is returned`() =
        test {
            val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
            whenever(
                restClient.fetchPrompts(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(
                payload
            )

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate
            )

            assertThat(result.model).isNull()
            assertThat(result.error).isNull()
        }

    @Test
    fun `given prompts response with exception, when fetch prompts gets triggered, then prompts error is returned`() =
        test {
            val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
            whenever(
                restClient.fetchPrompts(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(
                payload
            )
            whenever(
                dao.insertForSite(
                    siteModel.id,
                    PROMPT_MODELS
                )
            ).thenThrow(IllegalStateException("Error"))

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate
            )

            assertThat(result.model).isNull()
            assertEquals(BloggingPromptsErrorType.GENERIC_ERROR, result.error.type)
            assertNull(result.error.message)
        }

    @Test
    fun `when get prompts is triggered, then a flow of prompt models is returned`() = test {
        whenever(dao.getAllPrompts(SITE_LOCAL_ID)).thenReturn(flowOf(PROMPT_ENTITIES))

        val result = promptsStore.getPrompts(siteModel).first()

        assertThat(result).isEqualTo(BloggingPromptsResult(PROMPT_MODELS))
    }

    @Test
    fun `when get getPromptByDate is triggered, then a flow with a prompt model is returned`() =
        test {
            whenever(
                dao.getPromptForDate(
                    SITE_LOCAL_ID,
                    BloggingPromptsUtils.stringToDate("2015-01-13")
                )
            ).thenReturn(flowOf(listOf(SECOND_PROMPT_ENTITY)))

            val result = promptsStore.getPromptForDate(
                siteModel,
                BloggingPromptsUtils.stringToDate("2015-01-13")
            ).first()

            assertThat(result).isEqualTo(BloggingPromptsResult(SECOND_PROMPT_MODEL))
        }

    @Test
    fun `when get getPromptById is triggered, then a flow with a prompt model is returned`() =
        test {
            whenever(dao.getPrompt(SITE_LOCAL_ID, SECOND_PROMPT_MODEL.id)).thenReturn(
                flowOf(
                    listOf(THIRD_PROMPT_ENTITY)
                )
            )

            val result = promptsStore.getPromptById(
                siteModel,
                SECOND_PROMPT_MODEL.id
            ).first()

            assertThat(result).isEqualTo(BloggingPromptsResult(THIRD_PROMPT_MODEL))
        }

    @Test
    fun `given prompts error, when fetch prompts gets triggered, then prompts error is returned`() =
        test {
            val errorType = BloggingPromptsErrorType.API_ERROR
            val payload = BloggingPromptsPayload<BloggingPromptsListResponse>(
                BloggingPromptsError(
                    errorType
                )
            )
            whenever(
                restClient.fetchPrompts(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(payload)

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate
            )

            assertThat(result.model).isNull()
            assertEquals(errorType, result.error.type)
            assertNull(result.error.message)
        }
}
