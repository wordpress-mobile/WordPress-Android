package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoverBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice cover inner blocks
     */
    private static final Pattern PATTERN_COVER_INNER = Pattern.compile(new StringBuilder()
                    .append("(^.*?<div class=\"wp-block-cover__inner-container\">\\s*)")
                    .append("(.*)") // inner block contents
                    .append("(\\s*</div>\\s*</div>\\s*<!-- /wp:cover -->.*)").toString(), Pattern.DOTALL);

    /**
     * Pattern to match background-image url in cover block html content
     */
    private static final Pattern PATTERN_BACKGROUND_IMAGE_URL = Pattern.compile(
            "background-image:\\s*url\\([^\\)]+\\)");

    private final MediaUploadCompletionProcessor mMediaUploadCompletionProcessor;

    public CoverBlockProcessor(String localId, MediaFile mediaFile,
                               MediaUploadCompletionProcessor mediaUploadCompletionProcessor) {
        super(localId, mediaFile);
        mMediaUploadCompletionProcessor = mediaUploadCompletionProcessor;
    }

    @Override String processInnerBlock(String block) {
        Matcher innerMatcher = PATTERN_COVER_INNER.matcher(block);
        boolean innerCapturesFound = innerMatcher.find();

        // process inner contents recursively
        if (innerCapturesFound) {
            String innerProcessed = mMediaUploadCompletionProcessor.processContent(innerMatcher.group(2)); //
            return new StringBuilder()
                    .append(innerMatcher.group(1))
                    .append(innerProcessed)
                    .append(innerMatcher.group(3))
                    .toString();
        }

        return block;
    }

    @Override boolean processBlockJsonAttributes(JsonObject jsonAttributes) {
        if (jsonAttributes.get("id").getAsInt() == Integer.parseInt(mLocalId, 10)) {
            jsonAttributes.addProperty("id", Integer.parseInt(mRemoteId, 10));
            jsonAttributes.addProperty("url", mRemoteUrl);
            return true;
        }

        return false;
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select cover block div
        Element targetDiv = document.select(".wp-block-cover").first();

        // if a match is found, proceed with replacement
        if (targetDiv != null) {
            // replace background-image url in style attribute
            String style = PATTERN_BACKGROUND_IMAGE_URL.matcher(targetDiv.attr("style"))
                    .replaceFirst(String.format("background-image:url(%1$s)", mRemoteUrl));
            targetDiv.attr("style", style);

            // return injected block
            return true;
        }

        return false;
    }
}
