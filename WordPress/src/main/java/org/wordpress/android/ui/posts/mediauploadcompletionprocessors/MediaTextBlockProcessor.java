package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.Helpers;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaTextBlockProcessor implements BlockProcessor {
    private String mLocalId;
    private String mRemoteId;
    private String mRemoteUrl;

    /**
     * A {@link Pattern} to match media-text blocks with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The mLocalId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mMediaTextBlockPattern;

    public MediaTextBlockProcessor(String localId, MediaFile mediaFile) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
        mMediaTextBlockPattern = Helpers.getMediaTextBlockPattern(mLocalId);
    }

    @Override public String processBlock(String block) {
        Matcher matcher = mMediaTextBlockPattern.matcher(block);

        if (matcher.find()) {
            String headerComment = new StringBuilder()
                    .append(matcher.group(1))
                    .append(mRemoteId) // here we substitute remote id in place of the local id
                    .append(matcher.group(3))
                    .toString();
            String blockContent = matcher.group(4);
            String closingComment = matcher.group(5);

            // create document from block content
            Document document = Jsoup.parse(blockContent);
            document.outputSettings(OUTPUT_SETTINGS);

            // select image element with our local id
            Element targetImg = document.select("img").first();

            // if a match is found for img, proceed with replacement
            if (targetImg != null) {
                // replace attributes
                targetImg.attr("src", mRemoteUrl);

                // replace class
                targetImg.removeClass("wp-image-" + mLocalId);
                targetImg.addClass("wp-image-" + mRemoteId);

                // return injected block
                return new StringBuilder()
                        .append(headerComment)
                        .append(document.body().html()) // parser output
                        .append(closingComment)
                        .toString();
            } else { // try video
                // select video element with our local id
                Element targetVideo = document.select("video").first();

                // if a match is found for video, proceed with replacement
                if (targetVideo != null) {
                    // replace attribute
                    targetVideo.attr("src", mRemoteUrl);

                    // return injected block
                    return new StringBuilder()
                            .append(headerComment)
                            .append(document.body().html()) // parser output
                            .append(closingComment)
                            .toString();
                }
            }
        }

        // leave block unchanged
        return block;
    }
}
