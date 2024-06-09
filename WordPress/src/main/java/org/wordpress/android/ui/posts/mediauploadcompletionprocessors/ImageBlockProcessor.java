package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;


public class ImageBlockProcessor extends BlockProcessor {
    public ImageBlockProcessor(@NonNull String localId, @NonNull MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override
    public boolean processBlockContentDocument(@NonNull Document document) {
        // select image element with our local id
        Element targetImg = document.select("img").first();

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl);

            // replace class
            targetImg.removeClass("wp-image-" + localId);
            targetImg.addClass("wp-image-" + remoteId);

            return true;
        }

        return false;
    }

    @Override
    public boolean processBlockJsonAttributes(@NonNull JsonObject jsonAttributes) {
        JsonElement id = jsonAttributes.get("id");
        if (id != null && !id.isJsonNull() && id.getAsString().equals(localId)) {
            addIntPropertySafely(jsonAttributes, "id", remoteId);
            return true;
        }
        return false;
    }
}
