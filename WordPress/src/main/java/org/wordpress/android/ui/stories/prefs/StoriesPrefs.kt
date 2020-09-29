package org.wordpress.android.ui.stories.prefs

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StorySerializerUtils

object StoriesPrefs {
    private const val KEY_PREFIX_STORIES_SLIDE_ID = "story_slide_id-"
    private const val KEY_PREFIX_LOCAL_MEDIA_ID = "l-"
    private const val KEY_PREFIX_REMOTE_MEDIA_ID = "r-"

    private fun buildSlideKey(siteId: Long, mediaId: RemoteMediaId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_REMOTE_MEDIA_ID + mediaId.mediaId.toString()
    }

    private fun buildSlideKey(siteId: Long, mediaId: LocalMediaId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_LOCAL_MEDIA_ID + mediaId.id.toString()
    }

    @JvmStatic
    fun checkSlideIdExists(context: Context, siteId: Long, mediaId: RemoteMediaId): Boolean {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    @JvmStatic
    fun checkSlideOriginalBackgroundMediaExists(context: Context, siteId: Long, mediaId: RemoteMediaId): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlideWithRemoteId(context, siteId, mediaId)
        storyFrameItem?.let { frame ->
            // now check the background media exists or is accessible on this device
            frame.source.let { source ->
                if (source is FileBackgroundSource) {
                    source.file?.let {
                        return it.exists()
                    } ?: return false
                } else if (source is UriBackgroundSource) {
                    source.contentUri?.let {
                        return isUriAccessible(it, context)
                    } ?: return false
                }
            }
        }
        return false
    }

    private fun isUriAccessible(uri: Uri, context: Context): Boolean {
        if (uri.toString().startsWith("http")) {
            // TODO: assume it'll be accessible - we'll figure out later
            // potentially force external download using MediaUtils.downloadExternalMedia() here to ensure
            return true
        }
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStream.close()
                return true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun saveSlide(context: Context, slideIdKey: String, storySlideJson: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(slideIdKey, storySlideJson)
        editor.apply()
    }

    @JvmStatic
    fun isValidSlide(context: Context, siteId: Long, mediaId: RemoteMediaId): Boolean {
        return checkSlideIdExists(context, siteId, mediaId) &&
                checkSlideOriginalBackgroundMediaExists(context, siteId, mediaId)
    }

    private fun getSlideJson(context: Context, slideIdKey: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(slideIdKey, null)
    }

    @JvmStatic
    fun getSlideWithRemoteId(context: Context, siteId: Long, mediaId: RemoteMediaId): StoryFrameItem? {
        val jsonSlide = getSlideJson(context, buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    @JvmStatic
    fun getSlideWithLocalId(context: Context, siteId: Long, mediaId: LocalMediaId): StoryFrameItem? {
        val jsonSlide = getSlideJson(context, buildSlideKey(siteId, mediaId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun saveSlideWithLocalId(context: Context, siteId: Long, mediaId: LocalMediaId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(context, slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun saveSlideWithRemoteId(context: Context, siteId: Long, mediaId: RemoteMediaId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(context, slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun deleteSlideWithLocalId(context: Context, siteId: Long, mediaId: LocalMediaId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
    }

    fun deleteSlideWithRemoteId(context: Context, siteId: Long, mediaId: RemoteMediaId) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
    }

    @JvmStatic
    fun replaceLocalMediaIdKeyedSlideWithRemoteMediaIdKeyedSlide(
        context: Context,
        localIdKey: Long,
        remoteIdKey: Long,
        localSiteId: Long
    ) {
        // look for the slide saved with the local id key (mediaFile.id), and re-convert to mediaId.
        getSlideWithLocalId(
                context,
                localSiteId,
                LocalMediaId(localIdKey)
        )?.let {
            it.id = remoteIdKey.toString() // update the StoryFrameItem id to hold the same value as the remote mediaID
            saveSlideWithRemoteId(
                    context,
                    localSiteId,
                    RemoteMediaId(remoteIdKey), // use the new mediaId as key
                    it
            )
            // now delete the old entry
            deleteSlideWithLocalId(
                    context,
                    localSiteId,
                    LocalMediaId(localIdKey)
            )
        }
    }

    data class RemoteMediaId(val mediaId: Long)
    data class LocalMediaId(val id: Long)
}
