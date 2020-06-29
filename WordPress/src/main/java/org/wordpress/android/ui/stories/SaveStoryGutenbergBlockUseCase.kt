package org.wordpress.android.ui.stories

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

class SaveStoryGutenbergBlockUseCase @Inject constructor() {
    // TODO will be removed shortly, but keeping for sites that don't yet have support for the jetpack stories block.
    fun buildWPGallery(
        editPostRepository: EditPostRepository,
        mediaFiles: Map<String, MediaFile>
    ) {
        // Create a gallery shortcode and placeholders for Media Ids
        val idsString = mediaFiles.map {
            PostUtils.WP_STORIES_POST_MEDIA_LOCAL_ID_PLACEHOLDER + it.value.id.toString()
        }.joinToString(separator = ",")
        editPostRepository.update { postModel: PostModel ->
            postModel.setContent("[gallery type=\"slideshow\" ids=\"$idsString\"]")
            true
        }
    }
}
