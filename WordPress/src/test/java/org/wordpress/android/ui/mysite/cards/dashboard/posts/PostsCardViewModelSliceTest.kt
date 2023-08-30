package org.wordpress.android.ui.mysite.cards.dashboard.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostsCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var postsCardViewModelSlice: PostsCardViewModelSlice

    private val site = mock<SiteModel>()

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private val postId = 100

    @Before
    fun setUp() {
        postsCardViewModelSlice = PostsCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository
        )

        navigationActions = mutableListOf()
        postsCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `given draft post card, when post item is clicked, then post is opened for edit draft`() =
        test {
            val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

            params.onPostItemClick(
                MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams(
                    PostCardType.DRAFT,
                    postId
                )
            )

            assertThat(navigationActions).containsOnly(SiteNavigationAction.EditDraftPost(site, postId))
        }

    @Test
    fun `given scheduled post card, when post item is clicked, then post is opened for edit scheduled`() =
        test {
            val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

            params.onPostItemClick(
                MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams(
                    PostCardType.SCHEDULED,
                    postId
                )
            )

            assertThat(navigationActions).containsOnly(SiteNavigationAction.EditScheduledPost(site, postId))
        }

    @Test
    fun `given scheduled post card, when item is clicked, then event is tracked`() = test {
        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.onPostItemClick(
            MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams(
                PostCardType.SCHEDULED,
                postId
            )
        )

        verify(cardsTracker).trackPostItemClicked(PostCardType.SCHEDULED)
    }

    @Test
    fun `given draft post card, when item is clicked, then event is tracked`() = test {
        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.onPostItemClick(
            MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams(
                PostCardType.DRAFT,
                postId
            )
        )

        verify(cardsTracker).trackPostItemClicked(PostCardType.DRAFT)
    }
}
