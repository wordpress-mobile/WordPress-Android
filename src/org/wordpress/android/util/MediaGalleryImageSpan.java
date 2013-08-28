package org.wordpress.android.util;

import android.content.Context;
import android.text.style.ImageSpan;

import org.wordpress.android.R;
import org.wordpress.android.models.MediaGallery;

public class MediaGalleryImageSpan extends ImageSpan {

    private MediaGallery mMediaGallery;

    public MediaGalleryImageSpan(Context context, MediaGallery mediaGallery) {
        super(context, R.drawable.icon_mediagallery_placeholder);
        setMediaGallery(mediaGallery);
    }

    public MediaGallery getMediaGallery() {
        return mMediaGallery;
    }

    public void setMediaGallery(MediaGallery mediaGallery) {
        this.mMediaGallery = mediaGallery;
    }
    
}
