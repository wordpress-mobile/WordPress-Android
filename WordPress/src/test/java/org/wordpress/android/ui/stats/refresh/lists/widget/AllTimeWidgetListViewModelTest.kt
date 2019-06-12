package org.wordpress.android.ui.stats.refresh.lists.widget

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.stats.refresh.lists.widget.AllTimeWidgetListViewModel.AllTimeItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class AllTimeWidgetListViewModelTest {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var allTimeStore: AllTimeInsightsStore
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var site: SiteModel
    private lateinit var viewModel: AllTimeWidgetListViewModel
    private val siteId: Long = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT
    @Before
    fun setUp() {
        viewModel = AllTimeWidgetListViewModel(siteStore, allTimeStore, resourceProvider)
        viewModel.start(siteId, color.ordinal, appWidgetId)
    }

    @Test
    fun `builds ui model`() {
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)
        val viewsKey = "Views"
        val visitorsKey = "Visitors"
        val postsKey = "Posts"
        val bestKey = "Best"
        val views = 500
        val visitors = 100
        val posts = 50
        val viewsBestDayTotal = 300
        whenever(resourceProvider.getString(R.string.stats_views)).thenReturn(viewsKey)
        whenever(resourceProvider.getString(R.string.stats_visitors)).thenReturn(visitorsKey)
        whenever(resourceProvider.getString(R.string.posts)).thenReturn(postsKey)
        whenever(resourceProvider.getString(R.string.stats_insights_best_ever)).thenReturn(bestKey)
        whenever(allTimeStore.getAllTimeInsights(site)).thenReturn(
                InsightsAllTimeModel(
                        siteId,
                        null,
                        visitors,
                        views,
                        posts,
                        "Monday",
                        viewsBestDayTotal
                )
        )

        viewModel.onDataSetChanged { }

        assertThat(viewModel.data).hasSize(4)
        assertListItem(viewModel.data[0], viewsKey, views)
        assertListItem(viewModel.data[1], visitorsKey, visitors)
        assertListItem(viewModel.data[2], postsKey, posts)
        assertListItem(viewModel.data[3], bestKey, viewsBestDayTotal)
    }

    @Test
    fun `shows error when site is missing`() {
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(null)

        var onError: Int? = null
        viewModel.onDataSetChanged { onError = it }

        assertThat(onError).isEqualTo(appWidgetId)
    }

    private fun assertListItem(listItem: AllTimeItemUiModel, key: String, value: Int) {
        assertThat(listItem.layout).isEqualTo(R.layout.stats_views_widget_item_light)
        assertThat(listItem.localSiteId).isEqualTo(siteId)
        assertThat(listItem.key).isEqualTo(key)
        assertThat(listItem.value).isEqualTo(value.toString())
    }
}