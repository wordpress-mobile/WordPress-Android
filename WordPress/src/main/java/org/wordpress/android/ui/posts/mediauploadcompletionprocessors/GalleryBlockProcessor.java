package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class GalleryBlockProcessor extends BlockProcessor {
    private String mAttachmentPageUrl;
    private String mLinkTo;

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private String mGalleryImageQuerySelector;

    public GalleryBlockProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        super(localId, mediaFile);
        mGalleryImageQuerySelector = new StringBuilder()
                .append("img[data-id=\"")
                .append(localId)
                .append("\"]")
                .toString();
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select image element with our local id
        Element targetImg = document.select(mGalleryImageQuerySelector).first();

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", mRemoteUrl);
            targetImg.attr("data-id", mRemoteId);
            targetImg.attr("data-full-url", mRemoteUrl);
            targetImg.attr("data-link", mAttachmentPageUrl);

            // replace class
            targetImg.removeClass("wp-image-" + mLocalId);
            targetImg.addClass("wp-image-" + mRemoteId);

            // set parent anchor href if necessary
            Element parent = targetImg.parent();
            if (parent != null && parent.is("a") && mLinkTo != null) {
                switch (mLinkTo) {
                    case "media":
                        parent.attr("href", mRemoteUrl);
                        break;
                    case "attachment":
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

    @Override boolean processBlockJsonAttributes(JsonObject jsonAttributes) {
        JsonArray ids = jsonAttributes.getAsJsonArray("ids");
        JsonElement linkTo = jsonAttributes.get("linkTo");
        if (linkTo != null) {
            mLinkTo = linkTo.getAsString();
        }
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).getAsString().equals(mLocalId)) {
                ids.set(i, new JsonPrimitive(Integer.parseInt(mRemoteId, 10)));
                return true;
            }
        }
        return false;
    }
}
