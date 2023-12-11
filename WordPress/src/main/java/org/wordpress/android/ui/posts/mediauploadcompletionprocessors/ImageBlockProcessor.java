package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.helpers.MediaFile;

import static org.wordpress.android.util.AppLog.T.MEDIA;

public class ImageBlockProcessor extends BlockProcessor {
    public ImageBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select image element with our local id
        Element targetImg = document.select("img").first();

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", mRemoteUrl);

            // replace class
            targetImg.removeClass("wp-image-" + mLocalId);
            targetImg.addClass("wp-image-" + mRemoteId);

            return true;
        }

        return false;
    }

    @Override boolean processBlockJsonAttributes(JsonObject jsonAttributes) {
        JsonElement id = jsonAttributes.get("id");
        if (id != null && !id.isJsonNull() && id.getAsString().equals(mLocalId)) {
            try {
                jsonAttributes.addProperty("id", Integer.parseInt(mRemoteId));
            } catch (NumberFormatException e) {
                AppLog.e(MEDIA, e.getMessage());
            }
            return true;
        }

        return false;
    }
}
