package org.wordpress.android.ui.posts

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

@Reusable
class UpdatePostCategoriesUseCase @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun updateCategories(categoryList: List<Long>, editPostRepository: EditPostRepository) {
        editPostRepository.updateAsync({ postModel ->
            postModel.setCategoryIdList(categoryList)
            true
        }, { postModel: PostImmutableModel?, result: UpdatePostResult ->
            if (result == Updated) {
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_CATEGORIES_ADDED)
            }
            null
        })
    }
}
