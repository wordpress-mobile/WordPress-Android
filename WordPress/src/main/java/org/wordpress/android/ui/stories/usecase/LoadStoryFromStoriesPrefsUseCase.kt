package org.wordpress.android.ui.stories.usecase

import android.net.Uri
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository
import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.RemoteMediaId
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

@Reusable
class LoadStoryFromStoriesPrefsUseCase @Inject constructor(
    private val storyRepositoryWrapper: StoryRepositoryWrapper,
    private val storiesPrefs: StoriesPrefs,
    private val mediaStore: MediaStore
) {
    fun getMediaIdsFromStoryBlockBridgeMediaFiles(mediaFiles: ArrayList<Any>): ArrayList<String> {
        val mediaIds = ArrayList<String>()
        for (mediaFile in mediaFiles) {
            val mediaIdLong = (mediaFile as HashMap<String?, Any?>)["id"]
                    .toString()
                    .toDouble() // this conversion is needed to strip off decimals that can come from RN
                    .toLong()
            val mediaIdString = mediaIdLong.toString()
            mediaIds.add(mediaIdString)
        }
        return mediaIds
    }

    fun areAllStorySlidesEditable(site: SiteModel, mediaIds: ArrayList<String>): Boolean {
        for (mediaId in mediaIds) {
            if (!storiesPrefs.isValidSlide(site.getId().toLong(), RemoteMediaId(mediaId.toLong()))) {
                return false
            }
        }
        return true
    }

    private fun loadOrReCreateStoryFromStoriesPrefs(site: SiteModel, mediaIds: ArrayList<String>): ReCreateStoryResult {
        // the StoryRepository didn't have it but we have editable serialized slides so,
        // create a new Story from scratch with these deserialized StoryFrameItems
        var allStorySlidesAreEditable: Boolean = true
        var noSlidesLoaded = false
        var storyIndex = StoryRepository.DEFAULT_NONE_SELECTED
        storyRepositoryWrapper.loadStory(storyIndex)
        storyIndex = storyRepositoryWrapper.getCurrentStoryIndex()
        for (mediaId in mediaIds) {
            var storyFrameItem = storiesPrefs.getSlideWithRemoteId(
                    site.getId().toLong(),
                    RemoteMediaId(mediaId.toLong())
            )
            if (storyFrameItem != null) {
                storyRepositoryWrapper.addStoryFrameItemToCurrentStory(storyFrameItem)
            } else {
                allStorySlidesAreEditable = false

                // for this missing frame we'll create a new frame using the actual uploaded flattened media
                val tmpMediaIdsLong = ArrayList<Long>()
                tmpMediaIdsLong.add(mediaId.toLong())
                val mediaModelList: List<MediaModel> = mediaStore.getSiteMediaWithIds(
                        site,
                        tmpMediaIdsLong
                )
                if (mediaModelList.size == 0) {
                    noSlidesLoaded = true
                } else {
                    for (mediaModel in mediaModelList) {
                        storyFrameItem = StoryFrameItem.getNewStoryFrameItemFromUri(
                                Uri.parse(mediaModel.url),
                                mediaModel.isVideo
                        )
                        storyFrameItem.id = mediaModel.mediaId.toString()
                        storyRepositoryWrapper.addStoryFrameItemToCurrentStory(storyFrameItem)
                    }
                }
            }
        }

        return ReCreateStoryResult(storyIndex, allStorySlidesAreEditable, noSlidesLoaded)
    }

    fun loadStoryFromMemoryOrRecreateFromPrefs(site: SiteModel, mediaFiles: ArrayList<Object>): ReCreateStoryResult {
        val mediaIds = getMediaIdsFromStoryBlockBridgeMediaFiles(
                mediaFiles
        )
        var allStorySlidesAreEditable = areAllStorySlidesEditable(
                site,
                mediaIds
        )

        // now look for a Story in the StoryRepository that has all these frames and, if not found, let's
        // just build the Story object ourselves to keep these files arrangement
        var storyIndex = storyRepositoryWrapper.findStoryContainingStoryFrameItemsByIds(mediaIds)
        if (storyIndex == StoryRepository.DEFAULT_NONE_SELECTED) {
            // the StoryRepository didn't have it but we have editable serialized slides so,
            // create a new Story from scratch with these deserialized StoryFrameItems
            return loadOrReCreateStoryFromStoriesPrefs(
                    site,
                    mediaIds
            )
        } else {
            return ReCreateStoryResult(storyIndex, allStorySlidesAreEditable, false)
        }
    }

    data class ReCreateStoryResult(
        val storyIndex: StoryIndex,
        val allStorySlidesAreEditable: Boolean,
        val noSlidesLoaded: Boolean
    )
}
