package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompt

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BloggingPromptUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardSource
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import java.util.Date

/* SITE */

const val SITE_LOCAL_ID = 1

/* MODEL */

private val PROMPT = BloggingPromptModel(
        id = 1234,
        text = "prompt text",
        title = "",
        content = "<!-- wp:pullquote -->\n" +
                "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the whole world live (on television or radio — choose your format). What would you say?</p><cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
                "<!-- /wp:pullquote -->",
        date = Date(),
        isAnswered = false,
        attribution = "",
        respondentsCount = 5,
        respondentsAvatarUrls = listOf()
)

@InternalCoroutinesApi
class BloggingPromptCardSourceTest : BaseUnitTest() {
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var bloggingPromptsStore: BloggingPromptsStore
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
    private lateinit var bloggingPromptCardSource: BloggingPromptCardSource

    private val data = BloggingPromptsResult(
            model = PROMPT
    )
    private val success = BloggingPromptsResult<List<BloggingPromptModel>>()
    private val apiError = BloggingPromptsResult<List<BloggingPromptModel>>(
            error = BloggingPromptsError(BloggingPromptsErrorType.API_ERROR)
    )

    @Before
    fun setUp() {
        init()
    }

    private fun init(isBloggingPromptFeatureEnabled: Boolean = true) {
        setUpMocks(isBloggingPromptFeatureEnabled)
        bloggingPromptCardSource = BloggingPromptCardSource(
                selectedSiteRepository,
                bloggingPromptsStore,
                bloggingPromptsFeatureConfig,
                TEST_DISPATCHER
        )
    }

    private fun setUpMocks(isBloggingPromptFeatureEnabled: Boolean) {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(isBloggingPromptFeatureEnabled)
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
    }

    /* GET DATA */

    @Test
    fun `when build is invoked, then start collecting prompts from store (database)`() = test {
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(bloggingPromptsStore).getPromptForDate(eq(siteModel), any())
    }

    @Test
    fun `given prompts feature disabled, no get or fetch calls are made`() = test {
        init(isBloggingPromptFeatureEnabled = false)
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        bloggingPromptCardSource.refresh()

        verify(bloggingPromptsStore, never()).getPromptForDate(any(), any())
        verify(bloggingPromptsStore, never()).fetchPrompts(any(), any(), any())
    }

    @Test
    fun `given build is invoked, when prompts are collected, then data is loaded (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(data.model))
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then prompts are fetched from store (network)`() = test {
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(bloggingPromptsStore).fetchPrompts(eq(siteModel), eq(20), any())
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get prompts (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(BloggingPromptsResult())
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(data.model))
    }

    @Test
    fun `given no error, when refresh is invoked, then data is only loaded from get prompts (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        bloggingPromptCardSource.refresh()

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(data.model))
    }
    /* IS REFRESHING */

    @Test
    fun `when build is invoked, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        bloggingPromptCardSource.refresh.observeForever { result.add(it) }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { result.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
        assertThat(result[3]).isTrue // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
    }

    @Test
    fun `given no error, when data has been refreshed, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { result.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
        assertThat(result[3]).isTrue // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
    }

    @Test
    fun `given error, when data has been refreshed, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPromptForDate(eq(siteModel), any())).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(apiError)
        bloggingPromptCardSource.refresh.observeForever {
            result.add(it)
        }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isFalse // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> error
        assertThat(result[3]).isTrue // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> error
    }
}
