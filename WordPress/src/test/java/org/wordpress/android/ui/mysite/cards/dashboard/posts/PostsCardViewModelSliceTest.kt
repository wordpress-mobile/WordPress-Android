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
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostsCardViewModelSlice.PostMenuCard
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostsCardViewModelSlice.PostMenuItemType
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostsCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var postCardBuilder: PostCardBuilder

    private lateinit var postsCardViewModelSlice: PostsCardViewModelSlice

    private val site = mock<SiteModel>()

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var uiModels : MutableList<List<MySiteCardAndItem.Card>?>

    private val postId = 100

    @Before
    fun setUp() {
        postsCardViewModelSlice = PostsCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            appPrefsWrapper,
            postCardBuilder
        )

        navigationActions = mutableListOf()
        postsCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        uiModels = mutableListOf()
        postsCardViewModelSlice.uiModel.observeForever { uiModels.add(it) }



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
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.POST.label,
                CardsTracker.PostSubtype.DRAFT.label
            )
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
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.POST.label,
                CardsTracker.PostSubtype.SCHEDULED.label
            )
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

        verify(cardsTracker).trackCardItemClicked(
            CardsTracker.Type.POST.label,
            CardsTracker.PostSubtype.SCHEDULED.label
        )
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

        verify(cardsTracker).trackCardItemClicked(CardsTracker.Type.POST.label, CardsTracker.PostSubtype.DRAFT.label)
    }

    @Test
    fun `given draft post card, when view all drafts posts is clicked, then draft posts screen is opened`() = test {
        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.moreMenuClickParams.onViewPostsMenuItemClick(PostCardType.DRAFT)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenDraftsPosts(site))
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            PostMenuCard.DRAFT_POSTS.label,
            PostMenuItemType.VIEW_ALL_DRAFTS.label
        )
    }

    @Test
    fun `given scheduled post card, when view all scheduled posts is clicked, then scheduled posts screen is opened`() =
        test {
            val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

            params.moreMenuClickParams.onViewPostsMenuItemClick(PostCardType.SCHEDULED)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenScheduledPosts(site))
            verify(cardsTracker).trackCardMoreMenuItemClicked(
                PostMenuCard.SCHEDULED_POSTS.label,
                PostMenuItemType.VIEW_ALL_SCHEDULED_POSTS.label
            )
        }

    @Test
    fun `given drafts post card, when more menu item hide this is accessed, then hide card is invoked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)

        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.moreMenuClickParams.onHideThisMenuItemClick.invoke(PostCardType.DRAFT)

        verify(appPrefsWrapper).setShouldHidePostDashboardCard(siteId, PostCardType.DRAFT.name, true)

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given drafts post card, when more menu item hide this is accessed, then hide card is tracked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)

        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.moreMenuClickParams.onHideThisMenuItemClick.invoke(PostCardType.DRAFT)

        verify(cardsTracker).trackCardMoreMenuItemClicked(
            PostMenuCard.DRAFT_POSTS.label,
            PostMenuItemType.HIDE_THIS.label
        )
    }

    @Test
    fun `given scheduled post card, when more menu item hide this is accessed, then hide card is invoked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)

        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.moreMenuClickParams.onHideThisMenuItemClick.invoke(PostCardType.SCHEDULED)

        verify(appPrefsWrapper).setShouldHidePostDashboardCard(siteId, PostCardType.SCHEDULED.name, true)

        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given scheduled post card, when more menu item hide this is accessed, then hide card is tracked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)

        val params = postsCardViewModelSlice.getPostsCardBuilderParams(mock())

        params.moreMenuClickParams.onHideThisMenuItemClick.invoke(PostCardType.SCHEDULED)

        verify(cardsTracker).trackCardMoreMenuItemClicked(
            PostMenuCard.SCHEDULED_POSTS.label,
            PostMenuItemType.HIDE_THIS.label
        )
    }
}
