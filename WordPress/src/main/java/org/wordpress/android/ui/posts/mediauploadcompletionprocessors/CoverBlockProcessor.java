package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_CAPTURES;

public class CoverBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice cover blocks
     */
    private static final String PATTERN_TEMPLATE_COVER = "(^\\s*?<!-- wp:cover \\{[^\\}]*?\"id\":)"
                                                         + "(\"?%1$s\"?)" // local id must match to be replaced
                                                         + "(.*? -->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:cover -->\n?)"; // closing comment

    /**
     * Template pattern used to match and splice cover inner blocks
     */
    private static final Pattern PATTERN_COVER_INNER = Pattern.compile("(^.*?<div class=\"wp-block-cover__inner-container\">\\s*)"
                                                           + "(.*)" // inner block contents
                                                           + "(\\s*</div>\\s*</div>\\s*<!-- /wp:cover -->.*)", Pattern.DOTALL);


    /**
     * Pattern to match background-image url in cover block html content
     */
    private static final Pattern PATTERN_BACKGROUND_IMAGE_URL = Pattern.compile("background-image:url\\([^\\)]+\\)");

    private final MediaUploadCompletionProcessor mMediaUploadCompletionProcessor;

    private String mBlockName;
    private String mBlockHeaderJson;
    private String mBlockContent;
    private String mClosingComment;

    public CoverBlockProcessor(String localId, MediaFile mediaFile,
                               MediaUploadCompletionProcessor mediaUploadCompletionProcessor) {
        super(localId, mediaFile);
        mMediaUploadCompletionProcessor = mediaUploadCompletionProcessor;
    }

    boolean matchAndSpliceBlockHeader(String block) {
        Matcher matcher = getMatcherForBlock(block);

        if (matcher.find()) {
            Matcher captures = PATTERN_BLOCK_CAPTURES.matcher(block);

            boolean capturesFound = captures.find();

            if (capturesFound) {
                mBlockName = captures.group(1);
                mBlockHeaderJson = captures.group(2);
                mBlockContent = captures.group(3);
                mClosingComment = captures.group(4);
            } else {
                mBlockName = null;
                mBlockHeaderJson = null;
                mBlockContent = null;
                mClosingComment = null;
            }

            return capturesFound;
        }

        return false;
    }

    /**
     * Processes a block returning a raw content replacement string. If a match is not found for the block content, this
     * method should return the original block contents unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    @Override
    String processBlock(String block) {
        if (matchAndSpliceBlockHeader(block)) {
            // create document from block content
            Document document = Jsoup.parse(mBlockContent);
            document.outputSettings(OUTPUT_SETTINGS);

            if (processBlockContentDocument(document)) {
                // return injected block
                return new StringBuilder()
                        .append("<!-- wp:")
                        .append(mBlockName)
                        .append(" ")
                        .append(processJson(mBlockHeaderJson))
                        .append(" -->\n")
                        .append(document.body().html()) // parser output
                        .append(mClosingComment)
                        .toString();
            }
        } else {
            Matcher innerMatcher = PATTERN_COVER_INNER.matcher(block);
            boolean innerCapturesFound = innerMatcher.find();

            // process inner contents recursively
            if (innerCapturesFound) {
                String innerProcessed = mMediaUploadCompletionProcessor.processPost(innerMatcher.group(2)); //
                return new StringBuilder()
                        .append(innerMatcher.group(1))
                        .append(innerProcessed)
                        .append(innerMatcher.group(3))
                        .toString();
            }
        }

        // leave block unchanged
        return block;
    }
    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_COVER;
    }

    String processJson(String blockJson) {
        JsonParser parser = new JsonParser();
        JsonObject attributes = parser.parse(blockJson).getAsJsonObject();
        attributes.addProperty("id", Integer.parseInt(mRemoteId, 10));
        attributes.addProperty("url", mRemoteUrl);
        return attributes.toString();
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
