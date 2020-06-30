package org.wordpress.android.ui.stories

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

class SaveStoryGutenbergBlockUseCase @Inject constructor() {
    // TODO will be removed shortly, but keeping for sites that don't yet have support for the jetpack stories block.
    fun buildWPGalleryInPost(
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

    fun buildJetpackStoryBlockInPost(
        editPostRepository: EditPostRepository,
        mediaFiles: Map<String, MediaFile>
    ) {
        val jsonArrayIds = ArrayList<Int>() // holds ids
        for (entry in mediaFiles.entries) {
            jsonArrayIds.add(entry.value.id)
        }
        var jsonArrayMediaFiles = ArrayList<StoryMediaFileData>() // holds media files
        for (entry in mediaFiles.entries) {
            jsonArrayMediaFiles.add(buildMediaFileData(entry.value))
        }

        val storyBlock = StoryBlockData(ids = jsonArrayIds, mediaFiles = jsonArrayMediaFiles)

        editPostRepository.update { postModel: PostModel ->
            postModel.setContent(createGBStoryBlockStringFromJson(storyBlock))
            true
        }
    }

    private fun buildMediaFileData(mediaFile: MediaFile): StoryMediaFileData {
        val mediaFileData = StoryMediaFileData(
                alt = "",
                id = mediaFile.id,
                link = StringUtils.notNullStr(mediaFile.fileURL),
                type = if (mediaFile.isVideo) "video" else "image",
                mime = StringUtils.notNullStr(mediaFile.mimeType),
                caption = "",
                url = StringUtils.notNullStr(mediaFile.fileURL)
        )
        return mediaFileData
    }

    fun replaceLocalMediaIdsWithRemoteMediaIdsInPost(post: PostModel, mediaFile: MediaFile) {
        // here we're going to first find the block header, obtain the JSON object, re-parse it, and re-build the block
        // WARNING note we're assuming to have only one Story block here so, beware of that!! this will find
        // the first match only, always. (shouldn't be a problem because we're always creating a new Post in the
        // app, but this won't make the cut if we decide to allow editing. Which we'll do by integrating with
        // the gutenberg parser / validator anyway.
        val content = post.content
        val jsonString: String = content.substring(
                content.indexOf(headingStart) + headingStart.length,
                content.indexOf(headingEnd)
        )
        val gson = Gson()
        val storyBlockData: StoryBlockData? = gson.fromJson(jsonString, StoryBlockData::class.java)

        // now replace matching localMediaId with remoteMediaId
        val localMediaId = mediaFile.id
        storyBlockData?.ids?.let {
            // update the ids list
            for (i in 0..it.size) {
                if (it[i] == localMediaId) {
                    it[i] = mediaFile.mediaId.toInt()
                    break
                }
            }
        }

        // now replace the same in the mediaFileObjects, obtain the URLs and replace
        storyBlockData?.mediaFiles?.filter { it.id == localMediaId }?.get(0)?.apply {
            id = mediaFile.mediaId.toInt()
            link = mediaFile.fileURL
            url = mediaFile.fileURL
        }
        post.setContent(createGBStoryBlockStringFromJson(requireNotNull(storyBlockData)))
    }

    private fun createGBStoryBlockStringFromJson(storyBlock: StoryBlockData): String {
        val gson = Gson()
        var newContent = headingStart + gson.toJson(storyBlock) + headingEnd + divPart + closingtag
        return newContent
    }

    data class StoryBlockData(
        var ids: MutableList<Int>,
        val mediaFiles: List<StoryMediaFileData>
    )

    data class StoryMediaFileData(
        val alt: String,
        var id: Int,
        var link: String,
        val type: String,
        val mime: String,
        val caption: String,
        var url: String
    )

    companion object {
        const val headingStart = "<!-- wp:jetpack/story "
        const val headingEnd = " -->\n"
        const val divPart = "<div class=\"wp-story wp-block-jetpack-story\"></div>\n"
        const val closingtag = "<!-- /wp:jetpack/story -->"
    }
}
