package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.wordpress.android.util.helpers.MediaFile;

import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.COVER;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.GALLERY;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.IMAGE;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.MEDIA_TEXT;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.VIDEO;

class BlockProcessorFactory {
    private final MediaUploadCompletionProcessor mMediaUploadCompletionProcessor;
    private final Map<MediaBlockType, BlockProcessor> mMediaBlockTypeBlockProcessorMap;

    /**
     * This factory initializes block processors for all media block types and provides a method to retrieve a block
     * processor instance for a given block type.
     */
    BlockProcessorFactory(MediaUploadCompletionProcessor mediaUploadCompletionProcessor) {
        mMediaUploadCompletionProcessor = mediaUploadCompletionProcessor;
        mMediaBlockTypeBlockProcessorMap = new HashMap<>();
    }

    /**
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     * @param siteUrl The site url - used to generate the attachmentPage url
     * @return The factory instance - useful for chaining this method upon instantiation
     */
    BlockProcessorFactory init(String localId, MediaFile mediaFile, String siteUrl) {
        mMediaBlockTypeBlockProcessorMap.put(IMAGE, new ImageBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(VIDEO, new VideoBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(MEDIA_TEXT, new MediaTextBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(GALLERY, new GalleryBlockProcessor(localId, mediaFile, siteUrl));
        mMediaBlockTypeBlockProcessorMap.put(COVER, new CoverBlockProcessor(localId, mediaFile,
                mMediaUploadCompletionProcessor));

        return this;
    }

    /**
     * Retrieves the block processor instance for the given media block type.
     *
     * @param blockType The media block type for which to provide a {@link BlockProcessor}
     * @return The {@link BlockProcessor} for the given media block type
     */
    BlockProcessor getProcessorForMediaBlockType(MediaBlockType blockType) {
        return mMediaBlockTypeBlockProcessorMap.get(blockType);
    }
}
