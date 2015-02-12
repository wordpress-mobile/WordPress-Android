package org.wordpress.android.util.helpers;

import android.content.Context;
import android.text.style.ImageSpan;

public class MediaGalleryImageSpan extends ImageSpan {
    private MediaGallery mMediaGallery;

    public MediaGalleryImageSpan(Context context, MediaGallery mediaGallery, int placeHolder) {
        super(context, placeHolder);
        setMediaGallery(mediaGallery);
    }

    public MediaGallery getMediaGallery() {
        return mMediaGallery;
    }

    public void setMediaGallery(MediaGallery mediaGallery) {
        this.mMediaGallery = mediaGallery;
    }
}
