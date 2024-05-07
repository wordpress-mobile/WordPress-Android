package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.PIE_CHART
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class FollowerTypesUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var followersStore: FollowersStore

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var site: SiteModel
    private lateinit var useCase: FollowerTypesUseCase

    private val wpModel = FollowersModel(3, emptyList(), false)
    private val emailModel = FollowersModel(7, emptyList(), false)
    private val wpComTitle = "WordPress"
    private val emailTitle = "Email"
    private val totalLabel = "Totals"
    private val contentDescriptionValue = "value, percentage of total followers"
    private val contentDescription = "title: $contentDescriptionValue"

    @Before
    fun setUp() {
        useCase = FollowerTypesUseCase(
            testDispatcher(),
            testDispatcher(),
            followersStore,
            statsSiteProvider,
            contentDescriptionHelper,
            statsUtils,
            resourceProvider
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)

        whenever(followersStore.getEmailFollowers(site, LimitMode.Top(0))).thenReturn(emailModel)
        whenever(followersStore.getWpComFollowers(site, LimitMode.Top(0))).thenReturn(wpModel)
        whenever(contentDescriptionHelper.buildContentDescription(any(), any())).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
        whenever(
            resourceProvider.getString(
                eq(R.string.stats_total_followers_content_description),
                any<Int>(),
                any<String>()
            )
        ).then { contentDescriptionValue }
        whenever(resourceProvider.getString(R.string.stats_followers_wordpress_com)).then { wpComTitle }
        whenever(resourceProvider.getString(R.string.email)).then { emailTitle }
        whenever(resourceProvider.getString(eq(R.string.stats_value_percent), any<String>(), any<String>()))
            .then { "${it.arguments[1]} (${it.arguments[2]}%)" }
        whenever(resourceProvider.getString(R.string.stats_follower_types_pie_chart_total_label)).then { totalLabel }
    }

    @Test
    fun `maps follower types to UI model`() = test {
        val forced = false
        val refresh = true

        whenever(followersStore.fetchEmailFollowers(site, PagedMode(0))).thenReturn(OnStatsFetched(emailModel))
        whenever(followersStore.fetchWpComFollowers(site, PagedMode(0))).thenReturn(OnStatsFetched(wpModel))

        val result = loadFollowerTypesData(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertThat(this[0].type).isEqualTo(PIE_CHART)

            for (i in 1..2) {
                assertThat(this[i].type).isEqualTo(LIST_ITEM)
                assertItem(this[i] as ListItem)
            }
        }
    }

    private fun assertItem(item: ListItem) {
        when (item.text) {
            wpComTitle -> assertThat(item.value).isEqualTo("3 (30%)")
            emailTitle -> assertThat(item.value).isEqualTo("7 (70%)")
        }
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    private suspend fun loadFollowerTypesData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
