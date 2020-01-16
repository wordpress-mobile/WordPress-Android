package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.wordpress.android.util.helpers.MediaFile;

import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.GALLERY;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.IMAGE;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.MEDIA_TEXT;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaBlockType.VIDEO;

class BlockProcessorFactory {
    private final Map<MediaBlockType, BlockProcessor> mMediaBlockTypeBlockProcessorMap;

    BlockProcessorFactory() {
        mMediaBlockTypeBlockProcessorMap = new HashMap<>();
    }

    BlockProcessorFactory init(String localId, MediaFile mediaFile, String siteUrl) {
        mMediaBlockTypeBlockProcessorMap.put(IMAGE, new ImageBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(VIDEO, new VideoBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(MEDIA_TEXT, new MediaTextBlockProcessor(localId, mediaFile));
        mMediaBlockTypeBlockProcessorMap.put(GALLERY, new GalleryBlockProcessor(localId, mediaFile, siteUrl));

        return this;
    }

    BlockProcessor getProcessorForMediaBlockType(MediaBlockType blockType) {
        return mMediaBlockTypeBlockProcessorMap.get(blockType);
    }
}
