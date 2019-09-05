package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class PostHeaderUseCaseTest : BaseUnitTest() {
    @Mock lateinit var statsPostProvider: StatsPostProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: PostHeaderUseCase
    private val postTitle: String = "Post title"
    private val postId: Long = 1L
    private val postUrl: String = "post_url.com"
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = PostHeaderUseCase(
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                statsPostProvider,
                tracker
        )
    }

    @Test
    fun `builds item without link`() = test {
        whenever(statsPostProvider.postTitle).thenReturn(postTitle)
        whenever(statsPostProvider.postId).thenReturn(postId)

        val result = loadData(refresh = true, forced = false)

        assertThat(result.data).isNotNull
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        (result.data!![0] as ReferredItem).let { referredItem ->
            assertThat(referredItem.label).isEqualTo(R.string.showing_stats_for)
            assertThat(referredItem.itemTitle).isEqualTo(postTitle)
            assertThat(referredItem.navigationAction).isNull()
        }
    }

    @Test
    fun `builds item with link when ID and URL are present`() = test {
        whenever(statsPostProvider.postTitle).thenReturn(postTitle)
        whenever(statsPostProvider.postId).thenReturn(postId)
        whenever(statsPostProvider.postUrl).thenReturn(postUrl)

        val result = loadData(refresh = true, forced = false)

        assertThat(result.data).isNotNull
        (result.data!![0] as ReferredItem).let { referredItem ->
            assertThat(referredItem.navigationAction).isNotNull

            var navigationTarget: NavigationTarget? = null
            useCase.navigationTarget.observeForever { navigationTarget = it?.getContentIfNotHandled() }
            referredItem.navigationAction!!.click()

            assertThat(navigationTarget).isNotNull
            (navigationTarget as ViewPost).let { viewPost ->
                assertThat(viewPost.postId).isEqualTo(postId)
                assertThat(viewPost.postUrl).isEqualTo(postUrl)
                assertThat(viewPost.postType).isEqualTo(StatsConstants.ITEM_TYPE_POST)
            }
        }
    }

    @Test
    fun `builds an empty item with no post`() = test {
        val result = loadData(refresh = true, forced = false)

        assertThat(result.data).isNull()
        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
