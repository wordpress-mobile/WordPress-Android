package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.nodes.Document.OutputSettings;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.util.helpers.MediaFile;

/**
 * Abstract class to be extended for each enumerated {@link MediaBlockType}.
 */
abstract class BlockProcessor {
    /**
     * HTML output used by the parser
     */
    @SuppressWarnings("checkstyle:LineLength") static final OutputSettings OUTPUT_SETTINGS = new OutputSettings()
            .outline(false)
//          .syntax(Syntax.xml)
//            Do we want xml or html here (e.g. self closing tags, boolean attributes)?
//            https://stackoverflow.com/questions/26584974/keeping-html-boolean-attributes-in-their-original-form-when-parsing-with-jsoup
            .prettyPrint(false);

    String mLocalId;
    String mRemoteId;
    String mRemoteUrl;

    /**
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     */
    BlockProcessor(String localId, MediaFile mediaFile) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
    }

    /**
     * Processes a block returning a raw content replacement string. If a match is not found for the block content, this
     * method should return the original block contents unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    abstract String processBlock(String block);
}
