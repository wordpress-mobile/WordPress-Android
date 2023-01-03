package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BloggingPromptUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
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
            "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the whole world" +
            " live (on television or radio — choose your format). What would you say?</p><cite>(courtesy of" +
            " plinky.com)</cite></blockquote></figure>\n" +
            "<!-- /wp:pullquote -->",
    date = Date(),
    isAnswered = false,
    attribution = "",
    respondentsCount = 5,
    respondentsAvatarUrls = listOf()
)

@ExperimentalCoroutinesApi
class BloggingPromptCardSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var bloggingPromptsStore: BloggingPromptsStore

    @Mock
    private lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var bloggingRemindersStore: BloggingRemindersStore
    private lateinit var bloggingPromptCardSource: BloggingPromptCardSource

    private val data = BloggingPromptsResult(
        model = listOf(PROMPT)
    )
    private val success = BloggingPromptsResult<List<BloggingPromptModel>>()
    private val apiError = BloggingPromptsResult<List<BloggingPromptModel>>(
        error = BloggingPromptsError(BloggingPromptsErrorType.API_ERROR)
    )
    private var siteModel = SiteModel().apply {
        id = SITE_LOCAL_ID
        setIsPotentialBloggingSite(true)
    }

    private val bloggingReminderSettings = BloggingRemindersModel(
        siteId = SITE_LOCAL_ID,
        isPromptIncluded = true
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
            appPrefsWrapper,
            bloggingRemindersStore,
            testDispatcher()
        )
    }

    private fun setUpMocks(isBloggingPromptFeatureEnabled: Boolean) {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(isBloggingPromptFeatureEnabled)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(null)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(flowOf(bloggingReminderSettings))
    }

    /* GET DATA */

    @Test
    fun `when build is invoked, then start collecting prompts from store (database)`() = test {
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(bloggingPromptsStore).getPrompts(eq(siteModel))
    }

    @Test
    fun `given prompts feature disabled, no get or fetch calls are made`() = test {
        init(isBloggingPromptFeatureEnabled = false)
        val result = mutableListOf<BloggingPromptUpdate>()

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        bloggingPromptCardSource.refresh()

        verify(bloggingPromptsStore, never()).getPrompts(eq(siteModel))
        verify(bloggingPromptsStore, never()).fetchPrompts(any(), any(), any())
    }

    @Test
    fun `given build is invoked, when prompts are collected, then data is loaded (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
    }

    /* REFRESH DATA */

    @Test
    fun `when build is invoked, then prompts are fetched from store (network)`() = test {
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        advanceUntilIdle()

        verify(bloggingPromptsStore).fetchPrompts(eq(siteModel), eq(20), any())
    }

    @Test
    fun `given no error, when build is invoked, then data is only loaded from get prompts (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(BloggingPromptsResult())
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
    }

    @Test
    fun `given no error, when refresh is invoked, then data is only loaded from get prompts (database)`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        bloggingPromptCardSource.refresh()

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
    }

    /* SKIPPED PROMPT */

    @Test
    fun `given build is invoked, when prompt is skipped, then empty state is loaded`() = test {
        val result = mutableListOf<BloggingPromptUpdate>()
        whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(Date())
        bloggingPromptCardSource.refresh.observeForever { }

        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(BloggingPromptUpdate(null))
    }

    /* SITE BASED PROMPT AVAILABILITY LOGIC */

    @Test
    fun `on build, if prompt not skipped, prompt reminder opted-in then prompt is loaded`() =
        test {
            val result = mutableListOf<BloggingPromptUpdate>()
            whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(
                flowOf(
                    BloggingRemindersModel(
                        siteId = 1,
                        isPromptIncluded = true
                    )
                )
            )
            bloggingPromptCardSource.refresh.observeForever { }

            bloggingPromptCardSource.build(
                testScope(),
                SITE_LOCAL_ID
            ).observeForever { it?.let { result.add(it) } }

            assertThat(result.size).isEqualTo(1)
            assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
        }

    @Test
    fun `on build, if prompt not skipped, prompt reminder opted-out and site is blog then prompt is loaded`() =
        test {
            val result = mutableListOf<BloggingPromptUpdate>()
            val bloggingSite = SiteModel().apply {
                id = SITE_LOCAL_ID
                setIsPotentialBloggingSite(true)
            }
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(bloggingSite)
            whenever(bloggingPromptsStore.getPrompts(eq(bloggingSite))).thenReturn(flowOf(data))
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(
                flowOf(
                    BloggingRemindersModel(
                        siteId = 1,
                        isPromptIncluded = false
                    )
                )
            )
            bloggingPromptCardSource.refresh.observeForever { }

            bloggingPromptCardSource.build(
                testScope(),
                SITE_LOCAL_ID
            ).observeForever { it?.let { result.add(it) } }

            assertThat(result.size).isEqualTo(1)
            assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
        }

    @Test
    fun `on build, if prompt not skipped, prompt reminder opted-out and site is not blog then prompt is not loaded`() =
        test {
            val result = mutableListOf<BloggingPromptUpdate>()
            val bloggingSite = SiteModel().apply {
                id = SITE_LOCAL_ID
                setIsPotentialBloggingSite(false)
            }
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(bloggingSite)
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(
                flowOf(
                    BloggingRemindersModel(
                        siteId = 1,
                        isPromptIncluded = false
                    )
                )
            )
            bloggingPromptCardSource.refresh.observeForever { }

            bloggingPromptCardSource.build(
                testScope(),
                SITE_LOCAL_ID
            ).observeForever { it?.let { result.add(it) } }

            assertThat(result.size).isEqualTo(1)
            assertThat(result.first()).isEqualTo(BloggingPromptUpdate(null))
        }

    @Test
    fun `on build, if prompt not skipped, prompt reminder opted-in and site is not blog then prompt is loaded`() =
        test {
            val result = mutableListOf<BloggingPromptUpdate>()
            val bloggingSite = SiteModel().apply {
                id = SITE_LOCAL_ID
                setIsPotentialBloggingSite(false)
            }
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(bloggingSite)
            whenever(bloggingPromptsStore.getPrompts(eq(bloggingSite))).thenReturn(flowOf(data))
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).thenReturn(
                flowOf(
                    BloggingRemindersModel(
                        siteId = 1,
                        isPromptIncluded = true
                    )
                )
            )
            bloggingPromptCardSource.refresh.observeForever { }

            bloggingPromptCardSource.build(
                testScope(),
                SITE_LOCAL_ID
            ).observeForever { it?.let { result.add(it) } }

            assertThat(result.size).isEqualTo(1)
            assertThat(result.first()).isEqualTo(BloggingPromptUpdate(PROMPT))
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
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { result.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
    }

    @Test
    fun `when refreshTodayPrompt is invoked, single prompt refresh is called`() = test {
        val regularRefreshResult = mutableListOf<Boolean>()
        val singlePromptRefreshResult = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success).thenReturn(success)
        bloggingPromptCardSource.singleRefresh.observeForever { singlePromptRefreshResult.add(it) }
        bloggingPromptCardSource.refresh.observeForever { regularRefreshResult.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refreshTodayPrompt()
        advanceUntilIdle()

        assertThat(singlePromptRefreshResult.size).isEqualTo(1)
        assertThat(singlePromptRefreshResult[0]).isFalse // init
    }

    @Test
    fun `when refreshTodayPrompt is invoked, nothing happens if refresh is already in progress`() = test {
        val regularRefreshResult = mutableListOf<Boolean>()
        val singlePromptRefreshResult = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        // we do not return success from bloggingPromptsStore.fetchPrompts() which locks live data in refreshing state
        bloggingPromptCardSource.singleRefresh.observeForever { singlePromptRefreshResult.add(it) }
        bloggingPromptCardSource.refresh.observeForever { regularRefreshResult.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refreshTodayPrompt()

        assertThat(singlePromptRefreshResult.size).isEqualTo(1)
        assertThat(singlePromptRefreshResult[0]).isFalse // init
    }

    @Test
    fun `given no error, when data has been refreshed, then refresh is set to true`() = test {
        val result = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(success)
        bloggingPromptCardSource.refresh.observeForever { result.add(it) }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> success
    }

    @Test
    fun `given error, when data has been refreshed, then refresh is set to false`() = test {
        val result = mutableListOf<Boolean>()
        whenever(bloggingPromptsStore.getPrompts(eq(siteModel))).thenReturn(flowOf(data))
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any())).thenReturn(apiError)
        bloggingPromptCardSource.refresh.observeForever {
            result.add(it)
        }
        bloggingPromptCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        bloggingPromptCardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> bloggingPromptCardSource.fetchPrompts(...) -> error
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> bloggingPromptCardSource.fetchPrompts(...) -> error
    }
}
