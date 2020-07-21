package org.wordpress.android.ui.stories

import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.ui.stories.StoryComposerActivity.Companion
import javax.inject.Inject

class StoryComposerViewModel @Inject constructor(
    val postStore: PostStore,
    val savePostToDbUseCase: SavePostToDbUseCase
) :
        ViewModel() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var site: SiteModel
    fun start(editPostRepository: EditPostRepository) {
        this.editPostRepository = editPostRepository
    }


}
