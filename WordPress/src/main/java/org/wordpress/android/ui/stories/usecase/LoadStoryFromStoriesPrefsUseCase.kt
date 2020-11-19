package org.wordpress.android.ui.stories.usecase

import android.net.Uri
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository
import dagger.Reusable
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.TempId
import org.wordpress.android.util.StringUtils
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
            val rawIdField = (mediaFile as HashMap<String?, Any?>)["id"]
            if (rawIdField is String && rawIdField.startsWith(TEMPORARY_ID_PREFIX)) {
                mediaIds.add(rawIdField)
            } else {
                val mediaIdLong = rawIdField
                        .toString()
                        .toDouble() // this conversion is needed to strip off decimals that can come from RN
                        .toLong()
                val mediaIdString = mediaIdLong.toString()
                mediaIds.add(mediaIdString)
            }
        }
        return mediaIds
    }

    fun areAllStorySlidesEditable(site: SiteModel, mediaIds: ArrayList<String>): Boolean {
        for (mediaId in mediaIds) {
            // if this is not a remote nor a local / temporary slide, return false
            if (mediaId.startsWith(TEMPORARY_ID_PREFIX)) {
                if (!storiesPrefs.isValidSlide(site.id.toLong(), TempId(mediaId))) {
                    return false
                }
            } else {
                if (!storiesPrefs.isValidSlide(site.id.toLong(), RemoteId(mediaId.toLong())) &&
                        !storiesPrefs.isValidSlide(site.id.toLong(), LocalId(StringUtils.stringToInt(mediaId)))) {
                    return false
                }
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
            // let's check if this is a temporary id
            if (mediaId.startsWith(TEMPORARY_ID_PREFIX)) {
                storiesPrefs.getSlideWithTempId(
                        site.getId().toLong(),
                        TempId(mediaId)
                )?.let {
                    storyRepositoryWrapper.addStoryFrameItemToCurrentStory(it)
                }
            } else {
                storiesPrefs.getSlideWithRemoteId(
                        site.getId().toLong(),
                        RemoteId(mediaId.toLong())
                )?.let {
                    storyRepositoryWrapper.addStoryFrameItemToCurrentStory(it)
                } ?: run {
                    allStorySlidesAreEditable = false

                    // for this missing frame we'll create a new frame using the actual uploaded flattened media
                    val tmpMediaIdsLong = ArrayList<Long>()
                    tmpMediaIdsLong.add(mediaId.toLong())
                    val mediaModelList: List<MediaModel> = mediaStore.getSiteMediaWithIds(
                            site,
                            tmpMediaIdsLong
                    )
                    if (mediaModelList.isEmpty()) {
                        noSlidesLoaded = true
                    } else {
                        for (mediaModel in mediaModelList) {
                            val storyFrameItem = StoryFrameItem.getNewStoryFrameItemFromUri(
                                    Uri.parse(mediaModel.url),
                                    mediaModel.isVideo
                            )
                            storyFrameItem.id = mediaModel.mediaId.toString()
                            storyRepositoryWrapper.addStoryFrameItemToCurrentStory(storyFrameItem)
                        }
                    }
                }
            }
        }

        noSlidesLoaded = storyRepositoryWrapper.getStoryAtIndex(storyIndex).frames.size == 0

        return ReCreateStoryResult(storyIndex, allStorySlidesAreEditable, noSlidesLoaded)
    }

    fun loadStoryFromMemoryOrRecreateFromPrefs(site: SiteModel, mediaFiles: ArrayList<Any>): ReCreateStoryResult {
        val mediaIds = getMediaIdsFromStoryBlockBridgeMediaFiles(
                mediaFiles
        )
        var allStorySlidesAreEditable = areAllStorySlidesEditable(
                site,
                mediaIds
        )

        // now look for a Story in the StoryRepository that has all these frames and, if not found, let's
        // just build the Story object ourselves to match the order in which the media files were passed. 
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
