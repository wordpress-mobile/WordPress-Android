package org.wordpress.android.ui.stories.prefs

import android.annotation.SuppressLint
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
        private val KEY_STORIES_SLIDE_INCREMENTAL_ID = "incremental_id"
        private val KEY_PREFIX_STORIES_SLIDE_ID = "story_slide_id-"
        private val KEY_PREFIX_TEMP_MEDIA_ID = "t-"
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

    private fun buildSlideKey(siteId: Long, tempId: TempId): String {
        return KEY_PREFIX_STORIES_SLIDE_ID + siteId.toString() + "-" +
                KEY_PREFIX_TEMP_MEDIA_ID + tempId.id
    }

    @SuppressLint("ApplySharedPref")
    @Synchronized
    fun getNewIncrementalTempId(): Long {
        var currentIncrementalId = getIncrementalTempId()
        currentIncrementalId++
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putLong(KEY_STORIES_SLIDE_INCREMENTAL_ID, currentIncrementalId)
        editor.commit()
        return currentIncrementalId
    }

    fun getIncrementalTempId(): Long {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(
                        KEY_STORIES_SLIDE_INCREMENTAL_ID,
                        0
                )
    }

    fun checkSlideIdExists(siteId: Long, mediaId: RemoteId): Boolean {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    fun checkSlideIdExists(siteId: Long, tempId: TempId): Boolean {
        val slideIdKey = buildSlideKey(siteId, tempId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    fun checkSlideIdExists(siteId: Long, localId: LocalId): Boolean {
        val slideIdKey = buildSlideKey(siteId, localId)
        return PreferenceManager.getDefaultSharedPreferences(context).contains(slideIdKey)
    }

    fun checkSlideOriginalBackgroundMediaExists(siteId: Long, mediaId: RemoteId): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlideWithRemoteId(siteId, mediaId)
        return checkSlideOriginalBackgroundMediaExists(storyFrameItem)
    }

    fun checkSlideOriginalBackgroundMediaExists(siteId: Long, tempId: TempId): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlideWithTempId(siteId, tempId)
        return checkSlideOriginalBackgroundMediaExists(storyFrameItem)
    }

    fun checkSlideOriginalBackgroundMediaExists(siteId: Long, localId: LocalId): Boolean {
        val storyFrameItem: StoryFrameItem? = getSlideWithLocalId(siteId, localId)
        return checkSlideOriginalBackgroundMediaExists(storyFrameItem)
    }

    private fun checkSlideOriginalBackgroundMediaExists(storyFrameItem: StoryFrameItem?): Boolean {
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

    fun isValidSlide(siteId: Long, tempId: TempId): Boolean {
        return checkSlideIdExists(siteId, tempId) &&
                checkSlideOriginalBackgroundMediaExists(siteId, tempId)
    }

    fun isValidSlide(siteId: Long, localId: LocalId): Boolean {
        return checkSlideIdExists(siteId, localId) &&
                checkSlideOriginalBackgroundMediaExists(siteId, localId)
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

    fun getSlideWithTempId(siteId: Long, tempId: TempId): StoryFrameItem? {
        val jsonSlide = getSlideJson(buildSlideKey(siteId, tempId))
        jsonSlide?.let {
            return StorySerializerUtils.deserializeStoryFrameItem(jsonSlide)
        } ?: return null
    }

    fun saveSlideWithTempId(siteId: Long, tempId: TempId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, tempId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun saveSlideWithLocalId(siteId: Long, mediaId: LocalId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun saveSlideWithRemoteId(siteId: Long, mediaId: RemoteId, storyFrameItem: StoryFrameItem) {
        val slideIdKey = buildSlideKey(siteId, mediaId)
        saveSlide(slideIdKey, StorySerializerUtils.serializeStoryFrameItem(storyFrameItem))
    }

    fun deleteSlideWithTempId(siteId: Long, tempId: TempId) {
        val slideIdKey = buildSlideKey(siteId, tempId)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.remove(slideIdKey)
        editor.apply()
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

    fun replaceLocalMediaIdKeyedSlideWithRemoteMediaIdKeyedSlide_Phase2(
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

    fun replaceTempMediaIdKeyedSlideWithLocalMediaIdKeyedSlide_Phase1(
        tempId: TempId,
        localId: LocalId,
        localSiteId: Long
    ): StoryFrameItem? {
        // look for the slide saved with the local id key (mediaFile.id), and re-convert to mediaId.
        getSlideWithTempId(
                localSiteId,
                tempId
        )?.let {
            it.id = localId.value.toString()
            saveSlideWithLocalId(
                    localSiteId,
                    localId, // use the new localId as key
                    it
            )
            // now delete the old entry
            deleteSlideWithTempId(
                    localSiteId,
                    tempId
            )
            return it
        }
        return null
    }

    data class TempId(val id: String)
}
