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

    private fun buildSlideKey(siteId: Long, mediaId: Long): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" + mediaId.toString()
    }

    @JvmStatic
    fun checkSlideIdExists(context: Context, siteId: Long, mediaId: Long): Boolean {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    @JvmStatic
    fun checkSlideOriginalBackgroundMediaExists(context: Context, siteId: Long, mediaId: Long): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlide(context, siteId, mediaId)
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
    fun isValidSlide(context: Context, siteId: Long, mediaId: Long): Boolean {
        return checkSlideIdExists(context, siteId, mediaId) &&
                checkSlideOriginalBackgroundMediaExists(context, siteId, mediaId)
    }

    private fun getSlideJson(context: Context, siteId: Long, mediaId: Long): String? {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).getString(slideIdKey, null)
    }

    @JvmStatic
    fun getSlide(context: Context, siteId: Long, mediaId: Long): StoryFrameItem? {
        val jsonSlide = getSlideJson(context, siteId, mediaId)
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun saveSlide(context: Context, siteId: Long, mediaId: Long, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(context, slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun deleteSlide(context: Context, siteId: Long, mediaId: Long) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
    }
}
