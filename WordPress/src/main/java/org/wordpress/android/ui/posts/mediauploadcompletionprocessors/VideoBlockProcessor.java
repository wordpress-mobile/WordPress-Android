package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class VideoBlockProcessor extends BlockProcessor {
    public VideoBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select video element with our local id
        Element targetVideo = document.select("video").first();

        // if a match is found for video, proceed with replacement
        if (targetVideo != null) {
            // replace attribute
            targetVideo.attr("src", mRemoteUrl);

            // return injected block
            return true;
        }

        return false;
    }

    @Override boolean processBlockJsonAttributes(JsonObject jsonAttributes) {
        JsonElement id = jsonAttributes.get("id");
        if (id != null && id.getAsString().equals(mLocalId)) {
            jsonAttributes.addProperty("id", Integer.parseInt(mRemoteId));
            return true;
        }

        return false;
    }
}
