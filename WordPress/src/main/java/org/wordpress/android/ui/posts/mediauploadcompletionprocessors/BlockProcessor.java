package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.nodes.Document.OutputSettings;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.util.helpers.MediaFile;

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

    BlockProcessor(String localId, MediaFile mediaFile) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
    }

    /**
     * Processes a block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    abstract String processBlock(String block);
}
