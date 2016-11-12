package org.wordpress.android.ui.reader.utils;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * hash map of sizes of attachments in a reader post - created from the json "attachments" section
 * of the post endpoints
 */
public class ImageSizeMap extends HashMap<String, ImageSizeMap.ImageSize> {
    private static final String EMPTY_JSON = "{}";
    public ImageSizeMap(String jsonString) {
        if (TextUtils.isEmpty(jsonString) || jsonString.equals(EMPTY_JSON)) {
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> it = json.keys();
            if (!it.hasNext()) {
                return;
            }

            while (it.hasNext()) {
                JSONObject jsonAttach = json.optJSONObject(it.next());
                if (jsonAttach != null && JSONUtils.getString(jsonAttach, "mime_type").startsWith("image")) {
                    String normUrl = UrlUtils.normalizeUrl(UrlUtils.removeQuery(JSONUtils.getString(jsonAttach, "URL")));
                    int width = jsonAttach.optInt("width");
                    int height = jsonAttach.optInt("height");

                    // chech if data-orig-size is present and use it
                    String originalSize = jsonAttach.optString("data-orig-size", null);
                    if (originalSize != null) {
                        String[] sizes = originalSize.split(",");
                        if (sizes != null && sizes.length == 2) {
                            width = Integer.parseInt(sizes[0]);
                            height = Integer.parseInt(sizes[1]);
                        }
                    }

                    this.put(normUrl, new ImageSize(width, height));
                }
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.READER, e);
        }
    }

    public ImageSize getImageSize(final String imageUrl) {
        if (imageUrl == null) {
            return null;
        } else {
            return super.get(UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl)));
        }
    }

    public String getLargestImageUrl(int minImageWidth) {
        String currentImageUrl = null;
        int currentMaxWidth = minImageWidth;
        for (Entry<String, ImageSize> attach: this.entrySet()) {
            if (attach.getValue().width > currentMaxWidth) {
                currentImageUrl = attach.getKey();
                currentMaxWidth = attach.getValue().width;
            }
        }

        return currentImageUrl;
    }

    public static class ImageSize {
        public final int width;
        public final int height;
        public ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
