package org.wordpress.android.ui.media;

public enum MediaBrowserType {
    BROWSER,                 // browse & manage media
    EDITOR_PICKER,           // select multiple images or videos to insert into a post
    FEATURED_IMAGE_PICKER,   // select a single image as a featured image
    GRAVATAR_IMAGE_PICKER;   // select a single image as a gravatar

    public boolean isPicker() {
        return this != BROWSER;
    }

    public boolean isBrowser() {
        return this == BROWSER;
    }

    public boolean isSingleImagePicker() {
        return this == FEATURED_IMAGE_PICKER || this == GRAVATAR_IMAGE_PICKER;
    }

    public boolean canMultiselect() {
        return this == EDITOR_PICKER;
    }

    public boolean canFilter() {
        return this == BROWSER;
    }
}