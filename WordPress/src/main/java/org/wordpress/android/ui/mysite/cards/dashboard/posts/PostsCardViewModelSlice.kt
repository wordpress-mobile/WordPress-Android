package org.wordpress.android.ui.mysite.cards.dashboard.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.toSubtypeValue
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PostsCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val postCardBuilder: PostCardBuilder
) {
    private val _uiModel = MutableLiveData<List<MySiteCardAndItem.Card>?>()
    val uiModel = _uiModel as LiveData<List<MySiteCardAndItem.Card>?>

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun buildPostCard(postsCardModel: PostsCardModel?) {
        _uiModel.postValue(postCardBuilder.build(getPostsCardBuilderParams(postsCardModel)))
    }

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
        cardsTracker.trackCardMoreMenuClicked(postCardType.toPostMenuCardValue().label)
    }

    private fun onHideThisMenuItemClick(postCardType: PostCardType) {
        cardsTracker.trackCardMoreMenuItemClicked(
            postCardType.toPostMenuCardValue().label,
            PostMenuItemType.HIDE_THIS.label
        )
        appPrefsWrapper.setShouldHidePostDashboardCard(
            selectedSiteRepository.getSelectedSite()!!.siteId,
            postCardType.name,
            true
        )
        _uiModel.postValue(null)
    }

    private fun onViewPostsMenuItemClick(postCardType: PostCardType) {
        cardsTracker.trackCardMoreMenuItemClicked(
            card = postCardType.toPostMenuCardValue().label,
            item = when (postCardType) {
                PostCardType.DRAFT -> PostMenuItemType.VIEW_ALL_DRAFTS.label
                PostCardType.SCHEDULED -> PostMenuItemType.VIEW_ALL_SCHEDULED_POSTS.label
            }
        )
        onPostCardViewAllClick(postCardType)
    }

    private fun onPostItemClick(params: PostCardBuilderParams.PostItemClickParams) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            trackPostItemClicked(params.postCardType)
            when (params.postCardType) {
                PostCardType.DRAFT -> _onNavigation.value =
                    Event(SiteNavigationAction.EditDraftPost(site, params.postId))

                PostCardType.SCHEDULED -> _onNavigation.value =
                    Event(SiteNavigationAction.EditScheduledPost(site, params.postId))
            }
        }
    }

    private fun onPostCardViewAllClick(postCardType: PostCardType) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            _onNavigation.value = when (postCardType) {
                PostCardType.DRAFT -> Event(SiteNavigationAction.OpenDraftsPosts(site))
                PostCardType.SCHEDULED -> Event(SiteNavigationAction.OpenScheduledPosts(site))
            }
        }
    }

    private fun trackPostItemClicked(postCardType: PostCardType) {
        cardsTracker.trackCardItemClicked(CardsTracker.Type.POST.label, postCardType.toSubtypeValue().label)
    }

    enum class PostMenuItemType(val label: String) {
        VIEW_ALL_DRAFTS("view_all_drafts"),
        VIEW_ALL_SCHEDULED_POSTS("view_all_scheduled_posts"),
        HIDE_THIS("hide_this")
    }

    enum class PostMenuCard(val label: String) {
        DRAFT_POSTS("draft_posts"),
        SCHEDULED_POSTS("scheduled_posts")
    }

    private fun PostCardType.toPostMenuCardValue(): PostMenuCard {
        return when (this) {
            PostCardType.DRAFT -> PostMenuCard.DRAFT_POSTS
            PostCardType.SCHEDULED -> PostMenuCard.SCHEDULED_POSTS
        }
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
