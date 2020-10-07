package org.wordpress.android.ui.stories.prefs

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StorySerializerUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoriesPrefs @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_PREFIX_STORIES_SLIDE_ID = "story_slide_id-"
        private val KEY_PREFIX_LOCAL_MEDIA_ID = "l-"
        private val KEY_PREFIX_REMOTE_MEDIA_ID = "r-"
    }

    private fun buildSlideKey(siteId: Long, mediaId: RemoteId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_REMOTE_MEDIA_ID + mediaId.value.toString()
    }

    private fun buildSlideKey(siteId: Long, mediaId: LocalId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_LOCAL_MEDIA_ID + mediaId.value.toString()
    }

    fun checkSlideIdExists(siteId: Long, mediaId: RemoteId): Boolean {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    fun checkSlideOriginalBackgroundMediaExists(siteId: Long, mediaId: RemoteId): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlideWithRemoteId(siteId, mediaId)
        storyFrameItem?.let { frame ->
            // now check the background media exists or is accessible on this device
            frame.source.let { source ->
                if (source is FileBackgroundSource) {
                    source.file?.let {
                        return it.exists()
                    } ?: return false
                } else if (source is UriBackgroundSource) {
                    source.contentUri?.let {
                        return isUriAccessible(it)
                    } ?: return false
                }
            }
        }
        return false
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        if (uri.toString().startsWith("http")) {
            // TODO: assume it'll be accessible - we'll figure out later
            // potentially force external download using MediaUtils.downloadExternalMedia() here to ensure
            return true
        }
        try {
            context.contentResolver.openInputStream(uri)?.let {
                it.close()
                return true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun saveSlide(slideIdKey: String, storySlideJson: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(slideIdKey, storySlideJson)
        editor.apply()
    }

    fun isValidSlide(siteId: Long, mediaId: RemoteId): Boolean {
        return checkSlideIdExists(siteId, mediaId) &&
                checkSlideOriginalBackgroundMediaExists(siteId, mediaId)
    }

    private fun getSlideJson(slideIdKey: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(slideIdKey, null)
    }

    fun getSlideWithRemoteId(siteId: Long, mediaId: RemoteId): StoryFrameItem? {
        val jsonSlide = getSlideJson(buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun getSlideWithLocalId(siteId: Long, mediaId: LocalId): StoryFrameItem? {
        val jsonSlide = getSlideJson(buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun saveSlideWithLocalId(siteId: Long, mediaId: LocalId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun saveSlideWithRemoteId(siteId: Long, mediaId: RemoteId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun deleteSlideWithLocalId(siteId: Long, mediaId: LocalId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
    }

    fun deleteSlideWithRemoteId(siteId: Long, mediaId: RemoteId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
            remove(slideIdKey)
            apply()
        }
    }

    fun replaceLocalMediaIdKeyedSlideWithRemoteMediaIdKeyedSlide(
        localIdKey: Int,
        remoteIdKey: Long,
        localSiteId: Long
    ) {
        // look for the slide saved with the local id key (mediaFile.id), and re-convert to mediaId.
        getSlideWithLocalId(
                localSiteId,
                LocalId(localIdKey)
        )?.let {
            it.id = remoteIdKey.toString() // update the StoryFrameItem id to hold the same value as the remote mediaID
            saveSlideWithRemoteId(
                    localSiteId,
                    RemoteId(remoteIdKey), // use the new mediaId as key
                    it
            )
            // now delete the old entry
            deleteSlideWithLocalId(
                    localSiteId,
                    LocalId(localIdKey)
            )
        }
    }
}
