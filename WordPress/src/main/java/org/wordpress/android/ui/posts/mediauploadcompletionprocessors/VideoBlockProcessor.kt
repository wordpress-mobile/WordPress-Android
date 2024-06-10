package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class VideoBlockProcessor extends BlockProcessor {
    public VideoBlockProcessor(@NonNull String localId, @NonNull MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override
    public boolean processBlockContentDocument(@NonNull Document document) {
        // select video element with our local id
        Element targetVideo = document.select("video").first();

        // if a match is found for video, proceed with replacement
        if (targetVideo != null) {
            // replace attribute
            targetVideo.attr("src", remoteUrl);

            // return injected block
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
