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
            listOf(IMAGE).containsAll(allowedTypes) -> {
                ChooserContext.PHOTO to mimeTypes.getImageTypesOnly()
            }
            listOf(VIDEO).containsAll(allowedTypes) -> {
                ChooserContext.VIDEO to mimeTypes.getVideoTypesOnly()
            }
            listOf(IMAGE, VIDEO).containsAll(allowedTypes) -> {
                ChooserContext.PHOTO_OR_VIDEO to mimeTypes.getVideoAndImageTypesOnly()
            }
            listOf(AUDIO).containsAll(allowedTypes) -> {
                ChooserContext.AUDIO to mimeTypes.getAudioTypesOnly(planFor(site))
            }
            else -> {
                ChooserContext.MEDIA_FILE to mimeTypes.getAllTypes(planFor(site))
            }
        }
        return ChooserTypes(context, types.toList())
    }

    private fun planFor(site: SiteModel?) = mediaUtilsWrapper.getSitePlanForMimeTypes(site)

    data class ChooserTypes(val context: ChooserContext, val mimeTypes: List<String>)
}
