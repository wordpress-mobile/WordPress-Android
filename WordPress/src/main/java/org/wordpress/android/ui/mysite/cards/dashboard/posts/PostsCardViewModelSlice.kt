package org.wordpress.android.ui.mysite.cards.dashboard.posts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PostsCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun getPostsCardBuilderParams(postsCardModel: PostsCardModel?) : PostCardBuilderParams {
       return  PostCardBuilderParams(
            posts = postsCardModel,
            onPostItemClick = this::onPostItemClick,
            moreMenuClickParams = PostCardBuilderParams.MoreMenuParams(
               onMoreMenuClick = this::onMoreMenuClick,
               onHideThisMenuItemClick = this::onHideThisMenuItemClick,
               onViewPostsMenuItemClick = this::onViewPostsMenuItemClick
           )
        )
    }

    private fun onMoreMenuClick(postCardType: PostCardType) {
        // todo: annmarie implement cards tracker
        Log.i(javaClass.simpleName, "***=> onMoreMenuClick $postCardType")
    }

    private fun onHideThisMenuItemClick(postCardType: PostCardType) {
        // todo: annmarie implement logic
        // todo: annmarie implement cards tracker
        Log.i(javaClass.simpleName, "***=> onHideThisMenuItemClick $postCardType")
    }

    private fun onViewPostsMenuItemClick(postCardType: PostCardType) {
        // todo: annmarie implement logic
        // todo: annmarie implement cards tracker
        Log.i(javaClass.simpleName, "***=> onViewPostsMenuItemClick $postCardType")
    }

    private fun onPostItemClick(params: PostCardBuilderParams.PostItemClickParams) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            cardsTracker.trackPostItemClicked(params.postCardType)
            when (params.postCardType) {
                PostCardType.DRAFT -> _onNavigation.value =
                    Event(SiteNavigationAction.EditDraftPost(site, params.postId))

                PostCardType.SCHEDULED -> _onNavigation.value =
                    Event(SiteNavigationAction.EditScheduledPost(site, params.postId))
            }
        }
    }

    // todo: annmarie - repurpose for menu item click
//    private fun onPostCardFooterLinkClick(postCardType: PostCardType) {
//        selectedSiteRepository.getSelectedSite()?.let { site ->
//            cardsTracker.trackPostCardFooterLinkClicked(postCardType)
//            _onNavigation.value = when (postCardType) {
//                PostCardType.DRAFT -> Event(SiteNavigationAction.OpenDraftsPosts(site))
//                PostCardType.SCHEDULED -> Event(SiteNavigationAction.OpenScheduledPosts(site))
//            }
//        }
//    }
}
