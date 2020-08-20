package org.wordpress.android.ui.stories

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.ShortcodeUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

class SaveStoryGutenbergBlockUseCase @Inject constructor() {
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
                content.indexOf(HEADING_START) + HEADING_START.length,
                content.indexOf(HEADING_END)
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
        val mediaUrl = getMediaFileUrl(mediaFile)
        storyBlockData?.mediaFiles?.filter { it.id == localMediaId }?.get(0)?.apply {
            id = mediaFile.mediaId.toInt()
            link = mediaUrl
            url = mediaUrl
        }
        post.setContent(createGBStoryBlockStringFromJson(requireNotNull(storyBlockData)))
    }

    private fun getMediaFileUrl(mediaFile: MediaFile): String {
        if (VIDEOPRESS_MIME_TYPE.equals(mediaFile.mimeType)) {
            return VIDEOPRESS_BASE_URL + ShortcodeUtils.getVideoPressIdFromShortCode(mediaFile.videoPressShortCode) +
                    "/" + mediaFile.fileName
        } else {
            return StringUtils.notNullStr(mediaFile.fileURL)
        }
    }

    private fun createGBStoryBlockStringFromJson(storyBlock: StoryBlockData): String {
        val gson = Gson()
        return HEADING_START + gson.toJson(storyBlock) + HEADING_END + DIV_PART + CLOSING_TAG
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
        const val HEADING_START = "<!-- wp:jetpack/story "
        const val HEADING_END = " -->\n"
        const val DIV_PART = "<div class=\"wp-story wp-block-jetpack-story\"></div>\n"
        const val CLOSING_TAG = "<!-- /wp:jetpack/story -->"
        const val VIDEOPRESS_MIME_TYPE = "video/videopress"
        // TODO verify this base URL is constant - or update FluxC to bring the VideoPress video URL from REST API, i.e.
        // media_details.original.url
        // "media_details":{"original":{"url":"https:\/\/videos.files.wordpress.com\/Si7WwFlU\/wp-1597773400767.mp4"}
        const val VIDEOPRESS_BASE_URL = "https://videos.files.wordpress.com/"
    }
}
