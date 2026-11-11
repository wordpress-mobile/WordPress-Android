package org.wordpress.android.ui.mediapicker

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.ChooserContext
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.MediaUtilsWrapper
import javax.inject.Inject

/**
 * Resolves how the "Choose from device" action should open the OS picker for a given set of allowed
 * media types: which [ChooserContext] to use, the MIME types to request, and whether the selection
 * is ambiguous (mixes visual media with other files) and therefore needs a disambiguation step.
 */
class SystemPickerResolver @Inject constructor(
    private val mediaUtilsWrapper: MediaUtilsWrapper
) {
    private val mimeTypes = MimeTypes()

    /**
     * True when the selection allows both visual media (images/videos) and non-visual files
     * (audio/documents). Android's Photo Picker only surfaces visual media, so the user is asked
     * which kind they want before a picker is opened.
     */
    fun isAmbiguousMediaAndFileSelection(allowedTypes: Set<MediaType>): Boolean {
        val hasVisualMedia = allowedTypes.any { it == IMAGE || it == VIDEO }
        val hasOtherFiles = allowedTypes.any { it == AUDIO || it == DOCUMENT }
        return hasVisualMedia && hasOtherFiles
    }

    /**
     * Maps [allowedTypes] to the [ChooserContext] and MIME type list to hand to the OS picker.
     */
    fun resolveChooserContext(allowedTypes: Set<MediaType>, site: SiteModel?): ChooserTypes {
        val (context, types) = when {
            // Visual-media contexts use broad wildcards rather than our explicit accepted-subtype
            // list: on Android 13+ these route to the system Photo Picker, which filters album/cloud
            // contents by an exact match against EXTRA_MIME_TYPES. An explicit list (image/jpeg,
            // image/heic, ...) makes Google Photos albums appear empty because cloud items may not
            // report a matching subtype, so image/* and video/* are used to show all visual media.
            listOf(IMAGE).containsAll(allowedTypes) -> {
                ChooserContext.PHOTO to listOf(MIME_TYPE_IMAGE_ALL)
            }
            listOf(VIDEO).containsAll(allowedTypes) -> {
                ChooserContext.VIDEO to listOf(MIME_TYPE_VIDEO_ALL)
            }
            listOf(IMAGE, VIDEO).containsAll(allowedTypes) -> {
                ChooserContext.PHOTO_OR_VIDEO to listOf(MIME_TYPE_IMAGE_ALL, MIME_TYPE_VIDEO_ALL)
            }
            listOf(AUDIO).containsAll(allowedTypes) -> {
                ChooserContext.AUDIO to mimeTypes.getAudioTypesOnly(planFor(site)).toList()
            }
            else -> {
                ChooserContext.MEDIA_FILE to mimeTypes.getAllTypes(planFor(site)).toList()
            }
        }
        return ChooserTypes(context, types)
    }

    private fun planFor(site: SiteModel?) = mediaUtilsWrapper.getSitePlanForMimeTypes(site)

    data class ChooserTypes(val context: ChooserContext, val mimeTypes: List<String>)

    companion object {
        private const val MIME_TYPE_IMAGE_ALL = "image/*"
        private const val MIME_TYPE_VIDEO_ALL = "video/*"
    }
}
