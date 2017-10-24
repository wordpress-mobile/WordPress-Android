package org.wordpress.android.ui.media;

public enum MediaBrowserType {
    BROWSER,                   // browse & manage media
    EDITOR_PICKER,             // select multiple images or videos to insert into a post
    FEATURED_IMAGE_PICKER;     // select a single image to use as a post's featured image

    public boolean isPicker() {
        return this == EDITOR_PICKER || this == FEATURED_IMAGE_PICKER;
    }

    /*
     * multiselect is only availble when inserting into the editor
     */
    public boolean canMultiselect() {
        return this == EDITOR_PICKER;
    }

    /*
     * don't show non-image media when choosing featured image
     */
    public boolean imagesOnly() {
        return this == FEATURED_IMAGE_PICKER;
    }
}

