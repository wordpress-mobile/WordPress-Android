//Add WordPress image fields to ImageSpan object

package org.wordpress.android.util.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.style.ImageSpan;

public class WPImageSpan extends ImageSpan {
    protected Uri mImageSource = null;
    protected boolean mNetworkImageLoaded = false;
    protected MediaFile mMediaFile;

    public WPImageSpan(Context context, Bitmap b, Uri src) {
        super(context, b);
        this.mImageSource = src;
        mMediaFile = new MediaFile();
    }

    public WPImageSpan(Context context, int resId, Uri src) {
        super(context, resId);
        this.mImageSource = src;
        mMediaFile = new MediaFile();
    }

    public MediaFile getMediaFile() {
        return mMediaFile;
    }

    public void setMediaFile(MediaFile mMediaFile) {
        this.mMediaFile = mMediaFile;
    }

    public void setImageSource(Uri mImageSource) {
        this.mImageSource = mImageSource;
    }

    public Uri getImageSource() {
        return mImageSource;
    }

    public boolean isNetworkImageLoaded() {
        return mNetworkImageLoaded;
    }

    public void setNetworkImageLoaded(boolean networkImageLoaded) {
        this.mNetworkImageLoaded = networkImageLoaded;
    }
}
