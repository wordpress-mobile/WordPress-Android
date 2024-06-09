package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.wordpress.android.util.helpers.MediaFile

internal class BlockProcessorFactory
/**
 * This factory initializes block processors for all media block types and provides a method to retrieve a block
 * processor instance for a given block type.
 */(private val mMediaUploadCompletionProcessor: MediaUploadCompletionProcessor) {
    private val mMediaBlockTypeBlockProcessorMap: MutableMap<MediaBlockType, BlockProcessor> =
        HashMap()

    /**
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     * @param siteUrl The site url - used to generate the attachmentPage url
     * @return The factory instance - useful for chaining this method upon instantiation
     */
    fun init(localId: String, mediaFile: MediaFile, siteUrl: String): BlockProcessorFactory {
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.IMAGE] =
            ImageBlockProcessor(localId, mediaFile)
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.VIDEOPRESS] =
            VideoPressBlockProcessor(localId, mediaFile)
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.VIDEO] =
            VideoBlockProcessor(localId, mediaFile)
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.MEDIA_TEXT] =
            MediaTextBlockProcessor(localId, mediaFile)
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.GALLERY] = GalleryBlockProcessor(
            localId, mediaFile, siteUrl,
            mMediaUploadCompletionProcessor
        )
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.COVER] = CoverBlockProcessor(
            localId, mediaFile,
            mMediaUploadCompletionProcessor
        )
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.FILE] =
            FileBlockProcessor(localId, mediaFile)
        mMediaBlockTypeBlockProcessorMap[MediaBlockType.AUDIO] =
            AudioBlockProcessor(localId, mediaFile)

        return this
    }

    /**
     * Retrieves the block processor instance for the given media block type.
     *
     * @param blockType The media block type for which to provide a [BlockProcessor]
     * @return The [BlockProcessor] for the given media block type
     */
    fun getProcessorForMediaBlockType(blockType: MediaBlockType): BlockProcessor? {
        return mMediaBlockTypeBlockProcessorMap[blockType]
    }
}
