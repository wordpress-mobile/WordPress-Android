package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.util.AppLog.T.MEDIA;

public class GalleryBlockProcessor extends BlockProcessor {
    private final MediaUploadCompletionProcessor mMediaUploadCompletionProcessor;
    private String mAttachmentPageUrl;
    private String mLinkTo;

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private String mGalleryImageQuerySelector;

    /**
     * Template pattern used to match and splice inner image blocks in the refactored gallery format
     */
    private static final Pattern PATTERN_GALLERY_INNER = Pattern.compile(new StringBuilder()
            .append("(^.*?<figure class=\"[^\"]*?wp-block-gallery[^\"]*?\">\\s*)")
            .append("(.*)") // inner block contents
            .append("(\\s*</figure>\\s*<!-- /wp:gallery -->.*)").toString(), Pattern.DOTALL);

    public GalleryBlockProcessor(@NonNull String localId, @NonNull MediaFile mediaFile, String siteUrl,
                                 MediaUploadCompletionProcessor mediaUploadCompletionProcessor) {
        super(localId, mediaFile);
        mMediaUploadCompletionProcessor = mediaUploadCompletionProcessor;
        mGalleryImageQuerySelector = new StringBuilder()
                .append("img[data-id=\"")
                .append(localId)
                .append("\"]")
                .toString();
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
    }

    @Override
    public boolean processBlockContentDocument(@Nullable Document document) {
        // select image element with our local id
        Element targetImg = document.select(mGalleryImageQuerySelector).first();

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl);
            targetImg.attr("data-id", remoteId);
            targetImg.attr("data-full-url", remoteUrl);
            targetImg.attr("data-link", mAttachmentPageUrl);

            // replace class
            targetImg.removeClass("wp-image-" + localId);
            targetImg.addClass("wp-image-" + remoteId);

            // set parent anchor href if necessary
            Element parent = targetImg.parent();
            if (parent != null && parent.is("a") && mLinkTo != null) {
                switch (mLinkTo) {
                    case "file":
                        parent.attr("href", remoteUrl);
                        break;
                    case "post":
                        parent.attr("href", mAttachmentPageUrl);
                        break;
                    default:
                        return false;
                }
            }

            // return injected block
            return true;
        }

        return false;
    }

    @Override
    public boolean processBlockJsonAttributes(@Nullable JsonObject jsonAttributes) {
        // The new format does not have an `ids` attributes, so returning false here will defer to recursive processing
        JsonArray ids = jsonAttributes.getAsJsonArray("ids");
        if (ids == null || ids.isJsonNull()) {
            return false;
        }
        JsonElement linkTo = jsonAttributes.get("linkTo");
        if (linkTo != null && !linkTo.isJsonNull()) {
            mLinkTo = linkTo.getAsString();
        }
        for (int i = 0; i < ids.size(); i++) {
            JsonElement id = ids.get(i);
            if (id != null && !id.isJsonNull() && id.getAsString().equals(localId)) {
                try {
                    ids.set(i, new JsonPrimitive(Integer.parseInt(remoteId, 10)));
                } catch (NumberFormatException e) {
                    AppLog.e(MEDIA, e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String processInnerBlock(@NonNull String block) {
        Matcher innerMatcher = PATTERN_GALLERY_INNER.matcher(block);
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
}
