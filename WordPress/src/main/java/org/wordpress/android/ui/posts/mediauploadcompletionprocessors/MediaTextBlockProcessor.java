package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class MediaTextBlockProcessor extends BlockProcessor {
    public MediaTextBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override boolean processBlockContentDocument(Document document) {
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
            return true;
        } else { // try video
            // select video element with our local id
            Element targetVideo = document.select("video").first();

            // if a match is found for video, proceed with replacement
            if (targetVideo != null) {
                // replace attribute
                targetVideo.attr("src", mRemoteUrl);

                // return injected block
                return true;
            }
        }

        return false;
    }

    @Override boolean processBlockJsonAttributes(JsonObject jsonAttributes) {
        JsonElement id = jsonAttributes.get("mediaId");
        if (id != null && !id.isJsonNull() && id.getAsString().equals(mLocalId)) {
            jsonAttributes.addProperty("mediaId", Integer.parseInt(mRemoteId));
            return true;
        }

        return false;
    }
}
