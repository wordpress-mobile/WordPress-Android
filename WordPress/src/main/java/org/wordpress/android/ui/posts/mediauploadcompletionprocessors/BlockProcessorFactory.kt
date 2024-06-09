package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.wordpress.android.util.helpers.MediaFile

/**
 * This factory initializes block processors for all media block types and provides a method to retrieve a block
 * processor instance for a given block type.
 * @param localId The local media id that needs replacement
 * @param mediaFile The mediaFile containing the remote id and remote url`
 * @param siteUrl The site url - used to generate the attachmentPage url
 * @return The factory instance - useful for chaining this method upon instantiation
 */
internal class BlockProcessorFactory(
    mediaUploadCompletionProcessor: MediaUploadCompletionProcessor,
    localId: String,
    mediaFile: MediaFile,
    siteUrl: String
) {
    private val mediaBlockTypeBlockProcessorMap = hashMapOf(
        MediaBlockType.IMAGE to ImageBlockProcessor(localId, mediaFile),
        MediaBlockType.VIDEOPRESS to VideoPressBlockProcessor(localId, mediaFile),
        MediaBlockType.VIDEO to VideoBlockProcessor(localId, mediaFile),
        MediaBlockType.MEDIA_TEXT to MediaTextBlockProcessor(localId, mediaFile),
        MediaBlockType.GALLERY to GalleryBlockProcessor(localId, mediaFile, siteUrl, mediaUploadCompletionProcessor),
        MediaBlockType.COVER to CoverBlockProcessor(localId, mediaFile, mediaUploadCompletionProcessor),
        MediaBlockType.FILE to FileBlockProcessor(localId, mediaFile),
        MediaBlockType.AUDIO to AudioBlockProcessor(localId, mediaFile)
    )

    /**
     * Retrieves the block processor instance for the given media block type.
     *
     * @param blockType The media block type for which to provide a [BlockProcessor]
     * @return The [BlockProcessor] for the given media block type
     */
    fun getProcessorForMediaBlockType(blockType: MediaBlockType) = mediaBlockTypeBlockProcessorMap[blockType]
}
