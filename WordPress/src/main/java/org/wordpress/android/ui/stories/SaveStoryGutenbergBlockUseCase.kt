package org.wordpress.android.ui.stories

import android.text.TextUtils
import com.google.gson.Gson
import com.wordpress.stories.compose.frame.FrameIndex
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryIndex
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.TempId
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

class SaveStoryGutenbergBlockUseCase @Inject constructor(
    private val storiesPrefs: StoriesPrefs
) {
    fun buildJetpackStoryBlockInPost(
        editPostRepository: EditPostRepository,
        mediaFiles: ArrayList<MediaFile>
    ) {
        editPostRepository.update { postModel: PostModel ->
            postModel.setContent(buildJetpackStoryBlockString(mediaFiles))
            true
        }
    }

    private fun buildJetpackStoryBlockString(
        mediaFiles: List<MediaFile>
    ): String {
        val jsonArrayMediaFiles = ArrayList<StoryMediaFileData>() // holds media files
        for (mediaFile in mediaFiles) {
            jsonArrayMediaFiles.add(buildMediaFileData(mediaFile))
        }
        return buildJetpackStoryBlockStringFromStoryMediaFileData(jsonArrayMediaFiles)
    }

    fun buildJetpackStoryBlockStringFromStoryMediaFileData(
        storyMediaFileDataList: ArrayList<StoryMediaFileData>
    ): String {
        return createGBStoryBlockStringFromJson(StoryBlockData(mediaFiles = storyMediaFileDataList))
    }

    private fun buildMediaFileData(mediaFile: MediaFile): StoryMediaFileData {
        return StoryMediaFileData(
                alt = "",
                id = mediaFile.id.toString(),
                link = StringUtils.notNullStr(mediaFile.fileURL),
                type = if (mediaFile.isVideo) "video" else "image",
                mime = StringUtils.notNullStr(mediaFile.mimeType),
                caption = "",
                url = StringUtils.notNullStr(mediaFile.fileURL)
        )
    }

    fun buildMediaFileDataWithTemporaryId(mediaFile: MediaFile, temporaryId: String): StoryMediaFileData {
        return StoryMediaFileData(
                alt = "",
                id = temporaryId, // mediaFile.id,
                link = StringUtils.notNullStr(mediaFile.fileURL),
                type = if (mediaFile.isVideo) "video" else "image",
                mime = StringUtils.notNullStr(mediaFile.mimeType),
                caption = "",
                url = StringUtils.notNullStr(mediaFile.fileURL)
        )
    }

    fun buildMediaFileDataWithTemporaryIdNoMediaFile(
        temporaryId: String,
        url: String,
        isVideo: Boolean
    ): StoryMediaFileData {
        return StoryMediaFileData(
                alt = "",
                id = temporaryId, // mediaFile.id,
                link = url,
                type = if (isVideo) "video" else "image",
                mime = "",
                caption = "",
                url = url
        )
    }

    fun getTempIdForStoryFrame(tempIdBase: Long, storyIndex: StoryIndex, frameIndex: FrameIndex): String {
        return TEMPORARY_ID_PREFIX + "$tempIdBase-$storyIndex-$frameIndex"
    }

    fun findAllStoryBlocksInPostAndPerformOnEachMediaFilesJson(
        postModel: PostModel,
        listener: DoWithMediaFilesListener
    ) {
        var content = postModel.content
        // val contentMutable = StringBuilder(postModel.content)

        // find next Story Block
        // evaluate if this has a temporary id mediafile
        // --> remove mediaFiles entirely
        // set start index and go up.
        var storyBlockStartIndex = 0
        while (storyBlockStartIndex > -1 && storyBlockStartIndex < content.length) {
            storyBlockStartIndex = content.indexOf(HEADING_START, storyBlockStartIndex)
            if (storyBlockStartIndex > -1) {
                val storyBlockEndIndex = content.indexOf(HEADING_END, storyBlockStartIndex)
                val jsonString: String = content.substring(
                        storyBlockStartIndex + HEADING_START.length,
                        storyBlockEndIndex)
                content = listener.doWithMediaFilesJson(content, jsonString)
                storyBlockStartIndex += HEADING_START.length
            }
        }

        postModel.setContent(content)
    }

    fun replaceLocalMediaIdsWithRemoteMediaIdsInPost(postModel: PostModel, mediaFile: MediaFile) {
        if (TextUtils.isEmpty(mediaFile.mediaId)) {
            // if for any reason we couldn't obtain a remote mediaId, it's not worth spending time
            // looking to replace anything in the Post. Skip processing for later in error handling.
            return
        }
        val gson = Gson()
        findAllStoryBlocksInPostAndPerformOnEachMediaFilesJson(
                postModel,
                object : DoWithMediaFilesListener {
                    override fun doWithMediaFilesJson(content: String, mediaFilesJsonString: String): String {
                        var processedContent = content
                        val storyBlockData: StoryBlockData? =
                                gson.fromJson(mediaFilesJsonString, StoryBlockData::class.java)
                        storyBlockData?.let { storyBlockDataNonNull ->
                            val localMediaId = mediaFile.id.toString()
                            // now replace matching localMediaId with remoteMediaId in the mediaFileObjects, obtain the URLs and replace
                            val mediaFiles = storyBlockDataNonNull.mediaFiles.filter { it.id == localMediaId }
                            if (mediaFiles.isNotEmpty()) {
                                mediaFiles[0].apply {
                                    id = mediaFile.mediaId
                                    link = mediaFile.fileURL
                                    url = mediaFile.fileURL

                                    // look for the slide saved with the local id key (mediaFile.id), and re-convert to
                                    // mediaId.
                                    storiesPrefs.replaceLocalMediaIdKeyedSlideWithRemoteMediaIdKeyedSlide(
                                            mediaFile.id,
                                            mediaFile.mediaId.toLong(),
                                            postModel.localSiteId.toLong()
                                    )
                                }
                            }
                            processedContent = content.replace(mediaFilesJsonString, gson.toJson(storyBlockDataNonNull))
                        }
                        return processedContent
                    }
                }
        )
    }

    fun saveNewLocalFilesToStoriesPrefsTempSlides(
        site: SiteModel,
        storyIndex: StoryIndex,
        frames: ArrayList<StoryFrameItem>
    ) {
        for ((frameIndex, frame) in frames.withIndex()) {
            if (frame.id == null) {
                val assignedTempId = getTempIdForStoryFrame(
                        storiesPrefs.getNewIncrementalTempId(),
                        storyIndex,
                        frameIndex
                )
                frame.id = assignedTempId
            }
            storiesPrefs.saveSlideWithTempId(
                    site.id.toLong(),
                    TempId(requireNotNull(frame.id)), // should not be null at this point
                    frame
            )
        }
    }

    private fun createGBStoryBlockStringFromJson(storyBlock: StoryBlockData): String {
        val gson = Gson()
        return HEADING_START + gson.toJson(storyBlock) + HEADING_END + DIV_PART + CLOSING_TAG
    }

    interface DoWithMediaFilesListener {
        fun doWithMediaFilesJson(content: String, mediaFilesJsonString: String): String
    }

    data class StoryBlockData(
        val mediaFiles: List<StoryMediaFileData>
    )

    data class StoryMediaFileData(
        var alt: String,
        var id: String,
        var link: String,
        val type: String,
        val mime: String,
        val caption: String,
        var url: String
    )

    companion object {
        const val TEMPORARY_ID_PREFIX = "tempid-"
        const val HEADING_START = "<!-- wp:jetpack/story "
        const val HEADING_END = " -->\n"
        const val DIV_PART = "<div class=\"wp-story wp-block-jetpack-story\"></div>\n"
        const val CLOSING_TAG = "<!-- /wp:jetpack/story -->"
    }
}
