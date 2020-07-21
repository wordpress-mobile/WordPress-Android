package org.wordpress.android.ui.stories

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import javax.inject.Inject

class SaveInitialPostUseCase @Inject constructor(
    val postStore: PostStore,
    val savePostToDbUseCase: SavePostToDbUseCase
) {
    fun saveInitialPost(editPostRepository: EditPostRepository, site: SiteModel?) {
        editPostRepository.set {
            val post: PostModel = postStore.instantiatePostModel(site, false, null, null)
            post.setStatus(PostStatus.DRAFT.toString())
            post
        }
        editPostRepository.savePostSnapshot()
        // this is an artifact to be able to call savePostToDb()
        editPostRepository.getEditablePost()?.setPostFormat(StoryComposerActivity.POST_FORMAT_WP_STORY_KEY)
        site?.let {
            savePostToDbUseCase.savePostToDb(editPostRepository, it)
        }
    }
}
