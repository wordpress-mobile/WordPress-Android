package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.imageeditor.ImageEditor.EditorAction
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropDoneMenuClicked
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropOpened
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropSuccessful
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorCancelled
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorFinishedEditing
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorShown
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewCropMenuClicked
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewImageSelected
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewInsertImagesClicked
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject

@Reusable
class ImageEditorTracker @Inject constructor(
    private val context: Context,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackEditorAction(action: EditorAction) {
        val stat = when (action) {
            is EditorShown -> Stat.MEDIA_EDITOR_SHOWN
            is EditorFinishedEditing -> Stat.MEDIA_EDITOR_USED
            is EditorCancelled,
            is PreviewImageSelected,
            is PreviewInsertImagesClicked,
            is PreviewCropMenuClicked,
            is CropOpened,
            is CropDoneMenuClicked,
            is CropSuccessful -> null
        }

        val properties = when (action) {
            is EditorShown -> mapOf(NUMBER_OF_IMAGES to action.numOfImages)
            is EditorFinishedEditing -> {
                val distinctActions = ImageEditorInitializer.actions.distinct()
                if (distinctActions.isNotEmpty()) {
                    mapOf(ACTIONS to ImageEditorInitializer.actions.distinct().map { it.label })
                } else {
                    null
                }
            }
            is EditorCancelled,
            is PreviewImageSelected,
            is PreviewInsertImagesClicked,
            is PreviewCropMenuClicked,
            is CropOpened,
            is CropDoneMenuClicked,
            is CropSuccessful -> null
        }

        val noEditActionPerformed = stat == Stat.MEDIA_EDITOR_USED && properties == null
        if (stat == null || noEditActionPerformed) {
            return
        }

        if (properties == null) {
            analyticsTrackerWrapper.track(stat)
        } else {
            analyticsTrackerWrapper.track(stat, properties)
        }
    }

    fun trackAddPhoto(uris: List<Uri>) {
        for (uri in uris) {
            val properties = AnalyticsUtils.getMediaProperties(
                context,
                false,
                uri,
                null
            )
            analyticsTrackerWrapper.track(Stat.EDITOR_ADDED_PHOTO_VIA_MEDIA_EDITOR, properties)
        }
    }

    companion object {
        private const val ACTIONS = "actions"
        private const val NUMBER_OF_IMAGES = "number_of_images"
    }
}
