package org.wordpress.android.ui.media;

public enum MediaBrowserType {
    EDITOR_PICKER, // select multiple images or videos to insert into a post
    AZTEC_EDITOR_PICKER, // select multiple images or videos to insert into a post, hide source bar in portrait
    FEATURED_IMAGE_PICKER, // select a single image as a featured image
    GRAVATAR_IMAGE_PICKER, // select a single image as a gravatar
    SITE_ICON_PICKER, // select a single image as a site icon
    GUTENBERG_IMAGE_PICKER, // select one or multiple images from Gutenberg editor
    GUTENBERG_SINGLE_IMAGE_PICKER, // select image from Gutenberg editor
    GUTENBERG_VIDEO_PICKER, // select one or multiple videos from Gutenberg editor
    GUTENBERG_SINGLE_VIDEO_PICKER, // select video from Gutenberg editor
    GUTENBERG_SINGLE_MEDIA_PICKER, // select a single image or video to insert into a post
    GUTENBERG_MEDIA_PICKER, // select multiple images or videos to insert into a post
    GUTENBERG_SINGLE_FILE_PICKER, // select a file to insert into a post
    GUTENBERG_SINGLE_AUDIO_FILE_PICKER, // select an audio file to insert into a post
    FEEDBACK_FORM_MEDIA_PICKER; // select images or videos for support form

    public boolean isImagePicker() {
        return this == EDITOR_PICKER
               || this == AZTEC_EDITOR_PICKER
               || this == FEATURED_IMAGE_PICKER
               || this == GRAVATAR_IMAGE_PICKER
               || this == SITE_ICON_PICKER
               || this == GUTENBERG_IMAGE_PICKER
               || this == GUTENBERG_SINGLE_IMAGE_PICKER
               || this == GUTENBERG_SINGLE_MEDIA_PICKER
               || this == GUTENBERG_MEDIA_PICKER
               || this == GUTENBERG_SINGLE_FILE_PICKER
               || this == FEEDBACK_FORM_MEDIA_PICKER;
    }

    public boolean isVideoPicker() {
        return this == EDITOR_PICKER
               || this == AZTEC_EDITOR_PICKER
               || this == GUTENBERG_VIDEO_PICKER
               || this == GUTENBERG_SINGLE_VIDEO_PICKER
               || this == GUTENBERG_SINGLE_MEDIA_PICKER
               || this == GUTENBERG_MEDIA_PICKER
               || this == GUTENBERG_SINGLE_FILE_PICKER
               || this == FEEDBACK_FORM_MEDIA_PICKER;
    }

    public boolean isAudioPicker() {
        return this == GUTENBERG_SINGLE_FILE_PICKER || this == GUTENBERG_SINGLE_AUDIO_FILE_PICKER;
    }

    public boolean isDocumentPicker() {
        return this == GUTENBERG_SINGLE_FILE_PICKER;
    }

    public boolean isGutenbergPicker() {
        return this == GUTENBERG_IMAGE_PICKER
               || this == GUTENBERG_SINGLE_IMAGE_PICKER
               || this == GUTENBERG_VIDEO_PICKER
               || this == GUTENBERG_SINGLE_VIDEO_PICKER
               || this == GUTENBERG_SINGLE_MEDIA_PICKER
               || this == GUTENBERG_MEDIA_PICKER
               || this == GUTENBERG_SINGLE_FILE_PICKER
               || this == GUTENBERG_SINGLE_AUDIO_FILE_PICKER;
    }

    /**
     * True when this picker returns more than one media item to its caller.
     */
    public boolean canMultiselect() {
        return this == EDITOR_PICKER
               || this == AZTEC_EDITOR_PICKER
               || this == GUTENBERG_IMAGE_PICKER
               || this == GUTENBERG_VIDEO_PICKER
               || this == FEEDBACK_FORM_MEDIA_PICKER;
    }
}
