package org.wordpress.android.ui.prefs.homepage

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.SiteOptionsStore
import org.wordpress.android.fluxc.store.SiteOptionsStore.HomepageUpdatedPayload
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Data
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Loading
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsSelectorUiState.PageUiModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

class HomepageSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var homepageSettingsDataLoader: HomepageSettingsDataLoader
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var siteOptionsStore: SiteOptionsStore
    @Mock lateinit var dispatcher: Dispatcher
    private val siteModel = SiteModel()
    private val siteId = 1
    private val remoteId = 2L
    private val title = "title"
    private val localId = 3
    private lateinit var pageUiModel: PageUiModel
    private lateinit var localPages: List<PageUiModel>
    private lateinit var uiStates: MutableList<HomepageSettingsUiState>
    private var isDismissed: Boolean = false

    private lateinit var viewModel: HomepageSettingsViewModel

    @Before
    fun setUp() {
        viewModel = HomepageSettingsViewModel(
                Dispatchers.Unconfined,
                Dispatchers.Unconfined,
                dispatcher,
                homepageSettingsDataLoader,
                siteStore,
                siteOptionsStore
        )
        uiStates = mutableListOf()
        pageUiModel = PageUiModel(localId, remoteId, title)
        localPages = listOf(pageUiModel)
        isDismissed = false
        viewModel.dismissDialogEvent.observeForever { it?.applyIfNotHandled { isDismissed = true } }
    }

    @Test
    fun `initializes UI state for for classic blog`() = test {
        initSiteStore(true)

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel))
    }

    @Test
    fun `sets loading state when the loader is loading the data`() = test {
        initSiteStore(true, loadingResults = listOf(Loading))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel, isLoading = true)
        )
    }

    @Test
    fun `sets data state when loader loads the data with page for posts`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, pageForPostsId = remoteId, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageForPostsState = selectorWithSelection(),
                        pageOnFrontState = selectorWithoutSelection()
                )
        )
    }

    @Test
    fun `sets data state when loader loads the data with page on front`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, pageOnFrontId = remoteId, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection()
                )
        )
    }

    @Test
    fun `does not propagate data from the loader when page on front ID is not found`() = test {
        val invalidRemoteId = 5L
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, pageOnFrontId = invalidRemoteId, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel)
        )
    }

    @Test
    fun `does not propagate data from the loader when page for posts ID is not found`() = test {
        val invalidRemoteId = 5L
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, pageForPostsId = invalidRemoteId, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel)
        )
    }

    @Test
    fun `shows error from the loader`() = test {
        initSiteStore(true, loadingResults = listOf(LoadingResult.Error(R.string.error)))

        viewModel.start(siteId, null, null, null)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel, error = R.string.error)
        )
    }

    @Test
    fun `updates UI state to classic blog`() = test {
        initSiteStore(false)

        viewModel.start(siteId, null, null, null)

        viewModel.classicBlogSelected()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = false, siteModel = siteModel),
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel)
        )
    }

    @Test
    fun `updates UI state to static page`() = test {
        initSiteStore(true)

        viewModel.start(siteId, null, null, null)

        viewModel.staticHomepageSelected()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(isClassicBlogState = false, siteModel = siteModel)
        )
    }

    @Test
    fun `selects page on front`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        viewModel.onPageOnFrontSelected(localId)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection()
                )
        )
    }

    @Test
    fun `selects page for posts`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        viewModel.onPageForPostsSelected(localId)

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithSelection()
                )
        )
    }

    @Test
    fun `expands and hides dialog on drop down list click`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, null, null)

        viewModel.onPageOnFrontDialogOpened()
        viewModel.onPageForPostsDialogOpened()
        viewModel.onDialogHidden()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(isExpanded = true),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection(isExpanded = true)
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                )
        )
    }

    @Test
    fun `accepts correct state with classic blog`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(true, loadingResults = listOf(Data(listOf(pageForPosts))))
        whenever(siteOptionsStore.updateHomepage(eq(siteModel), any())).thenReturn(
                HomepageUpdatedPayload(
                        SiteHomepageSettings.Posts
                )
        )

        viewModel.start(siteId, null, null, null)

        viewModel.onAcceptClicked()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = true, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection(),
                        isLoading = true,
                        isSaveEnabled = false
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = true,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithoutSelection(),
                        pageForPostsState = selectorWithoutSelection()
                )
        )
    }

    @Test
    fun `accepts correct state with selected page on front`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(false, loadingResults = listOf(Data(listOf(pageForPosts))))
        whenever(siteOptionsStore.updateHomepage(eq(siteModel), any())).thenReturn(
                HomepageUpdatedPayload(
                        SiteHomepageSettings.Posts
                )
        )

        viewModel.start(siteId, null, null, remoteId)

        viewModel.onAcceptClicked()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = false, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection(),
                        isLoading = true,
                        isSaveEnabled = false
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection()
                )
        )

        verify(siteOptionsStore).updateHomepage(eq(siteModel), eq(SiteHomepageSettings.StaticPage(0, remoteId)))
        assertThat(isDismissed).isTrue()
    }

    @Test
    fun `fails if homepage and page for posts are the same`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(false, loadingResults = listOf(Data(listOf(pageForPosts))))

        viewModel.start(siteId, null, remoteId, remoteId)

        viewModel.onAcceptClicked()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = false, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithSelection(),
                        isLoading = true,
                        isSaveEnabled = false
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithSelection(),
                        error = R.string.site_settings_page_for_posts_and_homepage_cannot_be_equal
                )
        )

        verifyZeroInteractions(siteOptionsStore)
        assertThat(isDismissed).isFalse()
    }

    @Test
    fun `fails if API call fails`() = test {
        val pageForPosts = buildPage(pageUiModel)
        initSiteStore(false, loadingResults = listOf(Data(listOf(pageForPosts))))
        whenever(siteOptionsStore.updateHomepage(eq(siteModel), any())).thenReturn(
                HomepageUpdatedPayload(
                        SiteOptionsError(API_ERROR, "Api error")
                )
        )

        viewModel.start(siteId, null, null, remoteId)

        viewModel.onAcceptClicked()

        assertThat(uiStates).containsOnly(
                HomepageSettingsUiState(isClassicBlogState = false, siteModel = siteModel),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection()
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection(),
                        isLoading = true,
                        isSaveEnabled = false
                ),
                HomepageSettingsUiState(
                        isClassicBlogState = false,
                        siteModel = siteModel,
                        pageOnFrontState = selectorWithSelection(),
                        pageForPostsState = selectorWithoutSelection(),
                        error = R.string.site_settings_failed_update_homepage_settings
                )
        )

        verify(siteOptionsStore).updateHomepage(eq(siteModel), eq(SiteHomepageSettings.StaticPage(0, remoteId)))
        assertThat(isDismissed).isFalse()
    }

    @Test
    fun `on dismiss dismissed dialog`() = test {
        viewModel.onDismissClicked()

        assertThat(isDismissed).isTrue()
    }

    private fun selectorWithoutSelection(
        isExpanded: Boolean = false
    ): HomepageSettingsSelectorUiState {
        return HomepageSettingsSelectorUiState(
                localPages,
                UiStringRes(R.string.site_settings_select_page),
                0,
                isHighlighted = false,
                isExpanded = isExpanded
        )
    }

    private fun selectorWithSelection() = HomepageSettingsSelectorUiState(localPages, UiStringText(title), localId)

    private fun buildPage(pageUiModel: PageUiModel) =
            PageModel(
                    mock(),
                    siteModel,
                    pageUiModel.id,
                    pageUiModel.title,
                    PUBLISHED,
                    Date(),
                    false,
                    pageUiModel.remoteId,
                    null,
                    -1
            )

    private suspend fun initSiteStore(
        isClassicBlog: Boolean,
        pageForPostsId: Long? = null,
        pageOnFrontId: Long? = null,
        loadingResults: List<LoadingResult> = listOf()
    ) {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(siteModel)
        if (isClassicBlog) {
            siteModel.showOnFront = ShowOnFront.POSTS.value
        } else {
            siteModel.showOnFront = ShowOnFront.PAGE.value
        }
        siteModel.pageForPosts = pageForPostsId ?: 0
        siteModel.pageOnFront = pageOnFrontId ?: 0
        whenever(homepageSettingsDataLoader.loadPages(siteModel)).thenReturn(
                flow {
                    loadingResults.forEach { emit(it) }
                }
        )
        viewModel.uiState.observeForever { uiStates.add(it) }
    }
}
