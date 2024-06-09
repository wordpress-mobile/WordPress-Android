package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class MediaTextBlockProcessor extends BlockProcessor {
    public MediaTextBlockProcessor(@NonNull String localId, @NonNull MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override
    public boolean processBlockContentDocument(@NonNull Document document) {
        // select image element with our local id
        Element targetImg = document.select("img").first();

        // if a match is found for img, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl);

            // replace class
            targetImg.removeClass("wp-image-" + localId);
            targetImg.addClass("wp-image-" + remoteId);

            // return injected block
            return true;
        } else { // try video
            // select video element with our local id
            Element targetVideo = document.select("video").first();

            // if a match is found for video, proceed with replacement
            if (targetVideo != null) {
                // replace attribute
                targetVideo.attr("src", remoteUrl);

                // return injected block
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean processBlockJsonAttributes(@NonNull JsonObject jsonAttributes) {
        JsonElement id = jsonAttributes.get("mediaId");
        if (id != null && !id.isJsonNull() && id.getAsString().equals(localId)) {
            addIntPropertySafely(jsonAttributes, "mediaId", remoteId);
            return true;
        }

        return false;
    }
}
