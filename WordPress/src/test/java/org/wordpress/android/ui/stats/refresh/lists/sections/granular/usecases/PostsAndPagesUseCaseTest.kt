package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.HOMEPAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.PAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.POST
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val ITEMS_TO_LOAD = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

@ExperimentalCoroutinesApi
class PostsAndPagesUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var store: PostAndPageViewsStore

    @Mock
    lateinit var siteModelProvider: StatsSiteProvider

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var selectedDateProvider: SelectedDateProvider

    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper

    @Mock
    lateinit var statsUtils: StatsUtils
    private lateinit var useCase: PostsAndPagesUseCase
    private val contentDescription = "title, views"

    @Before
    fun setUp() {
        useCase = PostsAndPagesUseCase(
            statsGranularity,
            testDispatcher(),
            testDispatcher(),
            store,
            siteModelProvider,
            selectedDateProvider,
            tracker,
            contentDescriptionHelper,
            statsUtils,
            BLOCK
        )
        whenever(siteModelProvider.siteModel).thenReturn(site)
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
            SelectedDate(
                selectedDate,
                listOf(selectedDate)
            )
        )
        whenever(
            contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>(),
                any()
            )
        ).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `returns failed item when store fails`() = test {
        val forced = false
        val refresh = true
        val message = "error"
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
    }

    @Test
    fun `result contains only empty item when response is empty`() = test {
        val forced = false
        val refresh = true
        val emptyModel = PostAndPageViewsModel(listOf(), false)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(emptyModel))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.stateData!!
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_posts_and_pages)
        assertThat(items[1] is Empty).isTrue()
    }

    @Test
    fun `result converts post`() = test {
        val forced = false
        val refresh = true
        val post = ViewsModel(1L, "Post 1", 10, POST, "post.com")
        val model = PostAndPageViewsModel(listOf(post), false)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(3)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_posts_and_pages)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        val item = items[2] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_posts_white_24dp)
        assertThat(item.text).isEqualTo(post.title)
        assertThat(item.value).isEqualTo("10")
        assertThat(item.barWidth).isEqualTo(100)
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `result converts page`() = test {
        val forced = false
        val refresh = true
        val title = "Page 1"
        val views = 15
        val page = ViewsModel(2L, title, views, PAGE, "page.com")
        val model = PostAndPageViewsModel(listOf(page), false)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(3)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_posts_and_pages)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        val item = items[2] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(item.text).isEqualTo(title)
        assertThat(item.value).isEqualTo(views.toString())
        assertThat(item.barWidth).isEqualTo(100)
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `result converts home page`() = test {
        val forced = false
        val refresh = true
        val title = "Homepage 1"
        val views = 20
        val homePage = ViewsModel(3L, title, views, HOMEPAGE, "homepage.com")
        val model = PostAndPageViewsModel(listOf(homePage), false)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(3)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_posts_and_pages)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        val item = items[2] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(item.text).isEqualTo(title)
        assertThat(item.value).isEqualTo(views.toString())
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `shows divider between items`() = test {
        val forced = false
        val refresh = true
        val page = ViewsModel(2L, "Page 1", 10, PAGE, "page.com")
        val homePage = ViewsModel(3L, "Homepage 1", 20, HOMEPAGE, "homepage.com")
        val model = PostAndPageViewsModel(listOf(page, homePage), false)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(4)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat(items[3] is ListItemWithIcon).isTrue()
        assertThat((items[2] as ListItemWithIcon).showDivider).isEqualTo(true)
        assertThat((items[3] as ListItemWithIcon).showDivider).isEqualTo(false)
    }

    @Test
    fun `shows percentage on items`() = test {
        val forced = false
        val refresh = true
        val page = ViewsModel(2L, "Page 1", 10, PAGE, "page.com")
        val homePage = ViewsModel(3L, "Homepage 1", 20, HOMEPAGE, "homepage.com")
        val model = PostAndPageViewsModel(listOf(page, homePage), false)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(4)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat(items[3] is ListItemWithIcon).isTrue()
        val firstItem = items[2] as ListItemWithIcon
        assertThat(firstItem.showDivider).isEqualTo(true)
        assertThat(firstItem.barWidth).isEqualTo(50)
        val secondItem = items[3] as ListItemWithIcon
        assertThat(secondItem.showDivider).isEqualTo(false)
        assertThat(secondItem.barWidth).isEqualTo(100)
    }

    @Test
    fun `shows view more button when hasMore is true`() = test {
        val forced = false
        val refresh = true
        val id = 2L
        val url = "page.com"
        val page = ViewsModel(id, "Page 1", 10, PAGE, url)
        val hasMore = true
        val model = PostAndPageViewsModel(listOf(page), hasMore)
        whenever(
            store.getPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchPostAndPageViews(
                site,
                statsGranularity,
                LimitMode.Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        assertThat(result.type).isEqualTo(TimeStatsType.POSTS_AND_PAGES)
        val items = result.data!!
        assertThat(items.size).isEqualTo(4)
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat(items[3] is Link).isTrue()

        var navigationTarget: NavigationTarget? = null
        useCase.navigationTarget.observeForever { navigationTarget = it?.getContentIfNotHandled() }

        (items[3] as Link).navigateAction.click()

        assertThat(navigationTarget).isNotNull
        val viewPost = navigationTarget as ViewPostsAndPages
        assertThat(viewPost.statsGranularity).isEqualTo(statsGranularity)
    }

    private fun assertHeader(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_posts_and_pages_title_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_posts_and_pages_views_label)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
