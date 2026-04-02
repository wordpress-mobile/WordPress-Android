package org.wordpress.android.ui.reader.models;

import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;

/*
 * used by ReaderImageScanner to compile a list of images in a specific post
 */

public class ReaderImageList extends ArrayList<String> {
    private final boolean mIsPrivate;

    public ReaderImageList(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    // image urls are always added normalized and without query params for easier matching, and
    // to ensure there are no hard-coded sizes in the query
    private static String fixImageUrl(final String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        return UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl));
    }

    public int indexOfImageUrl(final String imageUrl) {
        if (imageUrl == null || this.isEmpty()) {
            return -1;
        }
        String fixedUrl = fixImageUrl(imageUrl);
        for (int i = 0; i < this.size(); i++) {
            if (fixedUrl.equals(this.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasImageUrl(final String imageUrl) {
        return (indexOfImageUrl(imageUrl) > -1);
    }

    public void addImageUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            this.add(fixImageUrl(imageUrl));
        }
    }

    public void addImageUrl(@SuppressWarnings("SameParameterValue") int index,
                            String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            this.add(index, fixImageUrl(imageUrl));
        }
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }
}
