package org.wordpress.android.ui.stories.prefs

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StorySerializerUtils
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

    private fun buildSlideKey(siteId: Long, mediaId: RemoteMediaId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_REMOTE_MEDIA_ID + mediaId.mediaId.toString()
    }

    private fun buildSlideKey(siteId: Long, mediaId: LocalMediaId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_LOCAL_MEDIA_ID + mediaId.id.toString()
    }

    fun checkSlideIdExists(siteId: Long, mediaId: RemoteMediaId): Boolean {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    fun checkSlideOriginalBackgroundMediaExists(siteId: Long, mediaId: RemoteMediaId): Boolean {
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

    fun isValidSlide(siteId: Long, mediaId: RemoteMediaId): Boolean {
        return checkSlideIdExists(siteId, mediaId) &&
                checkSlideOriginalBackgroundMediaExists(siteId, mediaId)
    }

    private fun getSlideJson(slideIdKey: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(slideIdKey, null)
    }

    fun getSlideWithRemoteId(siteId: Long, mediaId: RemoteMediaId): StoryFrameItem? {
        val jsonSlide = getSlideJson(buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun getSlideWithLocalId(siteId: Long, mediaId: LocalMediaId): StoryFrameItem? {
        val jsonSlide = getSlideJson(buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun saveSlideWithLocalId(siteId: Long, mediaId: LocalMediaId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun saveSlideWithRemoteId(siteId: Long, mediaId: RemoteMediaId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun deleteSlideWithLocalId(siteId: Long, mediaId: LocalMediaId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
    }

    fun deleteSlideWithRemoteId(siteId: Long, mediaId: RemoteMediaId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        PreferenceManager.getDefaultSharedPreferences(context).edit().apply { 
            remove(slideIdKey)
            apply()
        }
    }

    data class RemoteMediaId(val mediaId: Long)
    data class LocalMediaId(val id: Long)
}
